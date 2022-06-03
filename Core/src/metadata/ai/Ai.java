package metadata.ai;

import annotations.Name;
import annotations.Opt;
import metadata.MetadataItem;
import metadata.ai.features.Features;
import metadata.ai.features.trees.FeatureTrees;
import metadata.ai.heuristics.Heuristics;
import metadata.ai.misc.BestAgent;

//-----------------------------------------------------------------------------

/**
 * Defines metadata that can help AIs in the Ludii app to play this game at a
 * stronger level.
 * 
 * @remarks Specifying AI metadata for games is not mandatory.
 * 
 * @author Dennis Soemers and cambolbro
 */
public class Ai implements MetadataItem
{
	// WARNING: The weird capitalisation of of the class name is INTENTIONAL!
	// This makes the type name in the grammar and documentation look better,
	// as just "<ai>" instead of the really silly "<aI>" that we would get 
	// otherwise.
	
	//-------------------------------------------------------------------------
	
	/** Best agent for this game */
	private final BestAgent bestAgent;
	
	/** Heuristics */
	private final Heuristics heuristics;
	
	/** Automatically trained (or at least generated through some sort of automated process) heuristics */
	private final Heuristics trainedHeuristics;
	
	/** Features (could be handcrafted) */
	private final Features features;
	
	/** Automatically trained features */
	private final Features trainedFeatures;
	
	/** Automatically trained feature trees */
	private final FeatureTrees trainedFeatureTrees;
	
	//-------------------------------------------------------------------------

	/**
	 * Constructor
	 * @param bestAgent Can be used to specify the agent that is expected to
	 * perform best in this game. This algorithm will be used when the ``Ludii AI"
	 * option is selected in the Ludii app.
	 * @param heuristics Heuristics to be used by Alpha-Beta agents. These may be
	 * handcrafted heuristics.
	 * @param trainedHeuristics Heuristics trained or otherwise generated by some
	 * sort of automated process. Alpha-Beta agents will only use these if the previous
	 * parameter (for potentially handcrafted heuristics) is not used.
	 * @param features Feature sets (possibly handcrafted) to be used for biasing MCTS-based agents. 
	 * If not specified, Biased MCTS will not be available as an AI for this game in Ludii.
	 * @param trainedFeatures Automatically-trained feature sets. Will be used instead of the
	 * regular ``features'' parameter if that one is left unspecified.
	 * @param trainedFeatureTrees Automatically-trained decision trees of features. Will be used
	 * instead of the regular ``features'' or ``trainedFeatures'' parameters if those are left
	 * unspecified.
	 * 
	 * @example (ai (bestAgent "UCT"))
	 */
	public Ai
	(
				@Opt final BestAgent bestAgent,
				@Opt final Heuristics heuristics, 
		@Name 	@Opt final Heuristics trainedHeuristics,
				@Opt final Features features,
		@Name	@Opt final Features trainedFeatures,
		@Name	@Opt final FeatureTrees trainedFeatureTrees
	)
	{
		this.bestAgent = bestAgent;
		this.heuristics = heuristics;
		this.trainedHeuristics = trainedHeuristics;
		this.features = features;
		this.trainedFeatures = trainedFeatures;
		this.trainedFeatureTrees = trainedFeatureTrees;
	}

	//-------------------------------------------------------------------------
	
	/**
	 * @return Metadata item describing best agent
	 */
	public BestAgent bestAgent()
	{
		return bestAgent;
	}
	
	/**
	 * @return Heuristics for this game
	 */
	public Heuristics heuristics()
	{
		if (heuristics == null)
			return trainedHeuristics;
		return heuristics;
	}
	
	/**
	 * @return Automatically trained / generated heuristics for this game
	 */
	public Heuristics trainedHeuristics()
	{
		return trainedHeuristics;
	}
	
	/**
	 * @return Features for this game
	 */
	public Features features()
	{
		if (features == null)
			return trainedFeatures;
		
		return features;
	}
	
	/**
	 * @return Trained features for this game
	 */
	public Features trainedFeatures()
	{
		return trainedFeatures;
	}
	
	/**
	 * @return Trained feature trees for this game
	 */
	public FeatureTrees trainedFeatureTrees()
	{
		return trainedFeatureTrees;
	}
	
	//-------------------------------------------------------------------------

	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder();
		
		sb.append("    (ai\n");
		
		if (bestAgent != null)
			sb.append("        " + bestAgent.toString() + "\n");
			
		if (heuristics != null)
			sb.append("        " + heuristics.toString() + "\n");
		
		if (trainedHeuristics != null)
			sb.append("        trainedHeuristics:" + trainedHeuristics.toString() + "\n");
			
		if (features != null)
			sb.append("        " + features.toString() + "\n");
		
		if (trainedFeatures != null)
			sb.append("        trainedFeatures:" + trainedFeatures.toString() + "\n");
		
		if (trainedFeatureTrees != null)
			sb.append("        trainedFeatureTrees:" + trainedFeatureTrees.toString() + "\n");
			
		sb.append("    )\n");

		return sb.toString();
	}
	
	//-------------------------------------------------------------------------

}
