package Game_Solver;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
;

class NFG
{
	private StringBuffer buffer;
	private String filename;
	private int readhead;
	public ArrayList<String> playerNames;
	public ArrayList<Integer> numPlayerStrategies;
	public ArrayList<ArrayList<String>> playerStrategies;
	public ArrayList<ArrayList<Double>> playerPayoffs;
	
	/* constructor */
	 NFG(String fn){
		this.filename = fn;
		this.numPlayerStrategies = new ArrayList<Integer>();
		this.playerNames =  new ArrayList<String>();
		this.playerStrategies =  new ArrayList<ArrayList<String>>();
		this.playerPayoffs =  new ArrayList<ArrayList<Double>>();
		this.buffer = new StringBuffer();		
	}
	public void readNFGFile(){
		String fileLine;
		try{
			BufferedReader reader = new BufferedReader(new FileReader(this.filename));	
			while ((fileLine = reader.readLine()) != null) 
				this.buffer.append(fileLine+"\n");
		    reader.close();
        }
		catch (Exception e){
			System.out.println("readNFGFile: " + e);
		}
		//extract player names
		this.processPlayerNames();
		//extract strategies - either named OR # strategies per player
		this.processStrategies();
		//extract outcomes & outcome list OR payoff pairs (depending on file)
		this.processPayoffs();
	}		
	/* read player names from NFG header and add to the players list */
	private void processPlayerNames()
	{
		int beginIndex, endIndex;
		beginIndex = this.buffer.indexOf("{");
		if (beginIndex > 0) { 
			endIndex = this.buffer.indexOf("}", beginIndex+1);
				
			this.playerNames = new ArrayList<String> (Arrays.asList(this.buffer.substring(beginIndex+1, endIndex).trim().split("\"\\s+")));
			this.readhead = endIndex;
		}
		else  System.out.println("NFG: problem in processing player names.");
		
		for (int i = 0; i < this.playerNames.size(); i++)
		{
			if (this.playerNames.get(i).trim().length() < 1)
				this.playerNames.set(i, "_Player " + (i+1));
			this.playerNames.set(i,this.playerNames.get(i).replace("\"", ""));
			System.out.println("Playername: "+ this.playerNames.get(i));
		}				
	}
	
	private void processStrategyList(int beginIndex)
	{
		//find location of }}
		int endIndex = 0;
		Pattern pat = Pattern.compile("}\\s*}");
		Matcher m = pat.matcher(this.buffer.toString());
		
		if (m.find(beginIndex)){
			endIndex = m.end() -1;
			this.readhead = endIndex +1;
		}
		else
			System.out.println("NFGToXML Error: problem in processing strategy List.");
		//strategy list is substring between beginIndex and endIndex
		String strategyLine = this.buffer.substring(beginIndex, endIndex).trim();
		String[] strategies = strategyLine.split("\\}\\s*\\{");
		for(int i = 0; i < strategies.length; i++)
			this.parseStrategyNames(strategies[i]);
	}
	private void parseStrategyNames(String line)
	{
		line = line.replace("{", "");
		line = line.replace("}", "");line = line.trim();
		String[] tokens = line.split("\"\\s+\"");		
		ArrayList<String> Snames = new ArrayList<String>();				
		for(int i = 0; i < tokens.length; i++)
		{
			tokens[i] = tokens[i].replace("\"", "");
			if (! (tokens[i].trim().length() == 0)){
				Snames.add("\""+tokens[i]+"\"");
			}
			else   //for consistency of behavior, fill in blank strategy with placeholder
				Snames.add("\"_" + i+1 +"\"");		
		}		
		this.numPlayerStrategies.add(Snames.size());
		this.playerStrategies.add(Snames);
	}
	
	/* process list of strategies if they exist */	
	/* process information regarding strategy counts */
	
	private void processStrategyCounts(int beginIndex)
	{
		int endIndex = 0;
		String strategyLine;
		Pattern pat = Pattern.compile("}");
		Matcher m = pat.matcher(this.buffer.toString());
		if (m.find(beginIndex)){
			endIndex = m.end() -1;
			this.readhead = endIndex +1;
		}
		else
			System.out.println("NFGToXML Error: problem in processing strategy counts.");
		
		//strategy list is substring between beginIndex and endIndex
		strategyLine = this.buffer.substring(beginIndex, endIndex).trim();		
		String[] strategies = strategyLine.split("\\s+");
		for(int i = 0; i < strategies.length; i++)
			this.numPlayerStrategies.add(Integer.parseInt(strategies[i]));
	}
	//read strategies from file and call for XML processing 
	private void processStrategies()
	{
		//check to see if there is a second { in the line
		//if move one and find the next {
		//if there is a number after the {, then we have # of strategies
		//else if there is another { after (no number preceding) we have named strategies
		int beginIndex;		
		beginIndex = this.buffer.indexOf("{", this.readhead) + 1;
		if (beginIndex > 0)
		{ 
			int nextStart = this.buffer.indexOf("{", beginIndex);
			int nextEnd = this.buffer.indexOf("}", beginIndex);
			if ((nextStart < nextEnd) && (nextStart >= 0))  
				this.processStrategyList(beginIndex);
			else  
				this.processStrategyCounts(beginIndex);
		}
		else
			System.out.println("NFGToXML Error: problem in processing strategies.");
	}	
	/* process payoff information - payoffs are either formatted as "outcomes"
	 * or "payoff" pairs based on nfg file formats.
	 */
	private void processPayoffs()
	{
		int endIndex=0;
		int beginIndex = this.buffer.indexOf("{", this.readhead) + 1;
		if (beginIndex > 0) //outcomes structure  
			this.processOutcomePayoffs(beginIndex, endIndex);
		else //payoff pairs only
			this.processPayoffPairs(beginIndex, endIndex);
	}	
	/* tokenize and process strategy names */
	/* tokenize and process payoff values */
	private void parsePayoffLine(String line)
	{
		//need to know the number of strategies per player
		//then pick up each pair of payoffs and place in the right matrix position
		String[] tokens = line.split("\\s+");
		
		int numPlayers = this.playerNames.size();
		
		for (int j = 0; j < numPlayers; j++ )
		{
			//TODO improve efficiency of this code - place loop in different location?
			if (this.playerPayoffs.size() < numPlayers)
				this.playerPayoffs.add(new ArrayList<Double>());
			for (int i = j; i < tokens.length; i = i + numPlayers)
				this.playerPayoffs.get(j).add(Double.parseDouble(tokens[i].trim()));
		}
	}
	/* process payoffs of type outcome */
	private void processOutcomePayoffs(int beginIndex, int endIndex)
	{
		String payoffString;
		Pattern pat = Pattern.compile("}\\s*}");
		Matcher m = pat.matcher(this.buffer.toString());
		if (m.find(beginIndex))
		{
			this.readhead = m.end();
			endIndex = m.start();
		}
		else
			System.out.println("NFGToXML Error: problem in outcome payoffs.");
		
		String outcomesString = this.buffer.substring(beginIndex+1, endIndex).trim();
		String[] outcomes = outcomesString.split("\\}\\s*\\{");
		
		//get outcomes mapping
		String outcomeMapping = this.buffer.substring(this.readhead).trim();
		String[] mapping = outcomeMapping.split("\\s+");
		for (int k = 0; k < this.playerNames.size(); k++)
		{
			this.playerPayoffs.add(new ArrayList<Double>());
			for (int v = 0; v < outcomes.length; v++)
				this.playerPayoffs.get(k).add(0.0);
		}
		for(int i = 0; i < outcomes.length; i++)
		{
			Pattern pat2 = Pattern.compile("\"(.|\\s)*\"");
			Matcher m2 = pat2.matcher(outcomes[i]);
			if (m2.find())
				endIndex = m2.end();
			else
				System.out.println("NFGToXML Error: problem in processing outcome mapping.");
			int t = Integer.parseInt(mapping[i]) - 1;  	
			payoffString = outcomes[t].substring(endIndex);
			payoffString = payoffString.replace("\"", "");
			payoffString = payoffString.replace(",", "").trim();
			String[] payoffs = payoffString.split("\\s+");
			for (int j = 0; j < payoffs.length; j++)
				this.playerPayoffs.get(j).set(i, Double.parseDouble(payoffs[j].trim()));
		}
	}
	
	/* process payoffs of type payoff pairs */
	private void processPayoffPairs(int beginIndex, int endIndex){
		String payoffString = this.buffer.substring(this.readhead, this.buffer.length());
		this.parsePayoffLine(payoffString.trim());		
	}
	
}			
class StrategyProfile       
{
   
    private ArrayList <Double> payoffs;
    private ArrayList <Integer> profile;
    
    StrategyProfile(){
        payoffs = new ArrayList <Double>();
        profile = new ArrayList <Integer>();
    }
    
    ArrayList <Double> getPayoffs(){
        return this.payoffs;
    }
    
    ArrayList <Integer> getProfile(){
        return this.profile;
    }
    
    boolean compareProfile(int [] newprofile) //compares strategy profile to temp[] values
    {
    	System.out.println("profile.size: "+ this.profile.size());
        for(int i=0;i<this.profile.size();i++)
            if(profile.get(i)!=newprofile[i]) return false;
        return true;        //returns true if equal, false otherwise
    }
    
    	    //following functions used to store and get individual values
    void addPayoff(double payoff){
        System.out.println("payoff: "+payoff);
        payoffs.add(payoff);
    }
    
    double getPayoff(int index){
        return payoffs.get(index);
    }
    
    void addStrategy(int s){
    	System.out.println("strategy: "+s);
        profile.add(s);
    }
    
    int getStrategy(int index){
        return profile.get(index);
    }
}

public class Solver {

	public static void main(String[] args) throws IOException
	{
		System.out.println(args.length);
		String file = args[0];
	    System.out.println(file);
		NFG  game= new NFG(file);
		
		game.readNFGFile();
		int NoofStrategyProfiles = 1;
		int NumberofPlayer = game.playerNames.size();
		for(int i=0;i<game.numPlayerStrategies.size();i++)           
            NoofStrategyProfiles = NoofStrategyProfiles*game.numPlayerStrategies.get(i);
		
		System.out.println(Integer.toString(NoofStrategyProfiles)+" "+ Integer.toString(NumberofPlayer));

		StrategyProfile []  Profiles = new StrategyProfile[NoofStrategyProfiles];
		for(int i=0;i<NoofStrategyProfiles;i++) Profiles[i] = new StrategyProfile();
		int [] gamestrategy = new int[NumberofPlayer];
		for(int j=0;j<NumberofPlayer;j++) gamestrategy[j]=0;
		for(int i=0;i<NoofStrategyProfiles;i++)
		{
			int k= 0;
			for(int j=0;j<NumberofPlayer;j++)
			{
				System.out.println(gamestrategy[j]+" "+j);
				Profiles[i].addStrategy(gamestrategy[j]);
				Profiles[i].addPayoff(game.playerPayoffs.get(j).get(i));
			}
			while(k<NumberofPlayer && gamestrategy[k] == game.numPlayerStrategies.get(k)-1) {gamestrategy[k]=0;k++;}
			if(k<NumberofPlayer) gamestrategy[k] = gamestrategy[k] + 1; 
		}
		System.out.println("Hello");
		int [] stronglydominant = new int[NumberofPlayer];
		int [] weaklydominant = new int[NumberofPlayer];
		for(int i=0;i<NoofStrategyProfiles;i++)
			for(int j=0;j<NumberofPlayer;j++)
				System.out.println("action: "+Profiles[i].getStrategy(j)+" payoff: "+Profiles[i].getPayoff(j));
		for(int i=0;i<NumberofPlayer;i++)
		{
			stronglydominant[i]=weaklydominant[i]=-1;
			System.out.println("Player: "+ i + " Noofactions: " + game.numPlayerStrategies.get(i));
			for(int action=0;action<game.numPlayerStrategies.get(i);action++)
			{
				System.out.println("Player: "+ i + " action: "+action);
				int check=2,cnt=0;
				for(int k=0;k<NoofStrategyProfiles;k++)
				{
					if(Profiles[k].getStrategy(i) == action) {cnt++;continue;}
					int [] tmp = new int[NumberofPlayer];
					for(int ii=0;ii<NumberofPlayer;ii++) tmp[ii] = Profiles[k].getStrategy(ii);
					tmp[i] = action;int tocheck=-1;
					for(int l=0;l<NoofStrategyProfiles;l++)
						if(Profiles[l].compareProfile(tmp)) {tocheck=l;break;}
					System.out.println("tocheck: "+ tocheck + " action: "+action);
					if( Profiles[k].getPayoff(i) > Profiles[tocheck].getPayoff(i)) { check=0;break;}
					if( Profiles[k].getPayoff(i) == Profiles[tocheck].getPayoff(i)) { check=1;cnt++;}						
				}
				if(check==2) {stronglydominant[i] = action;break;}
				else if(check==1 && cnt<NoofStrategyProfiles) weaklydominant[i] = action;
				System.out.println("check: "+ check );							
			}
		}
		int dominantequilbrium=2;
		int [] equilbrium = new int[NumberofPlayer];
		for(int i=0;i<NumberofPlayer;i++)
			System.out.println(stronglydominant[i]+" "+weaklydominant[i]);
		for(int i=0;i<NumberofPlayer;i++)
		{
			if(stronglydominant[i]!=-1) equilbrium[i] =stronglydominant[i];
			else
			{
				dominantequilbrium = 1;
				if(weaklydominant[i]!=-1) equilbrium[i] =weaklydominant[i];
				else {dominantequilbrium = 0; break;}
			}

		}
		int dominantstrategy=0;
		if(dominantequilbrium!=0)
		{
			for(int l=0;l<NoofStrategyProfiles;l++)
				if(Profiles[l].compareProfile(equilbrium)) {dominantstrategy = l;break;}
		}
		if(dominantequilbrium==0)
			System.out.println("Equilbrium do not exit");
		else if(dominantequilbrium==1)
		{
			System.out.println("Weaklydominanty equilbrium exits with payoffs");
			for(int i=0;i<NumberofPlayer;i++)
				System.out.println(game.playerNames.get(i)+" payoff: "+ Profiles[dominantstrategy].getPayoff(i));
		}
		else if(dominantequilbrium==2)
		{
			System.out.println("Stronglydominanty equilbrium exits with payoffs");
			for(int i=0;i<NumberofPlayer;i++)
				System.out.println(game.playerNames.get(i)+" payoff: "+ Profiles[dominantstrategy].getPayoff(i));
		}			
	}
}


