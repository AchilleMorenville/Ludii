package gameDistance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import game.Game;
import gameDistance.datasets.Dataset;
import gameDistance.datasets.bagOfWords.BooleanConceptDataset;
import gameDistance.datasets.bagOfWords.NGramDataset;
import gameDistance.datasets.sequence.MoveConceptDataset;
import gameDistance.datasets.treeEdit.LudemeDataset;
import gameDistance.metrics.DistanceMetric;
import gameDistance.metrics.bagOfWords.Cosine;
import gameDistance.metrics.bagOfWords.Jaccard;
import gameDistance.metrics.bagOfWords.JensenShannonDivergence;
import gameDistance.metrics.sequence.GlobalAlignment;
import gameDistance.metrics.sequence.Levenshtein;
import gameDistance.metrics.sequence.LocalAlignment;
import gameDistance.metrics.sequence.RepeatedLocalAlignment;
import gameDistance.metrics.treeEdit.Apted;
import gameDistance.metrics.treeEdit.ZhangShasha;
import gameDistance.utils.DistanceUtils;
import main.DatabaseInformation;
import other.GameLoader;

/**
 * Compares all distance metrics for a given set of games.
 * 
 * Download the "TrialsRandom.zip" file from the Ludii Server.
 * Copy "TrialsRandom.zip" into "Ludii/Trials/", and extract the zip to a "TrialsRandom" folder (just right click and select "Extract Here"). Making the fullPath "Ludii/Trials/TrialsRandom/".
 * Run CompareAllDistanceMetrics.java
 * Output for each game/ruleset is stored in Ludii/Mining/res/gameDistance/
 * Make sure to set the "overrideStoredVocabularies" variable to true if any trials or games have changed.
 * 
 * @author matthew.stephenson
 */
public class CompareAllDistanceMetrics
{
	
	/** Set this variable to true, if the stored vocabularies should be overwritten on the next comparison. */
	final static boolean overrideStoredVocabularies = false;
	
	//---------------------------------------------------------------------

	final static String outputPath = "res/gameDistance/";
	
	final static Dataset ludemeDataset = new LudemeDataset();
	final static Dataset booleanConceptDataset = new BooleanConceptDataset();
	final static Dataset moveConceptDataset = new MoveConceptDataset();
	
	static Map<String, Double> fullLudemeVocabulary;
	static Map<String, Double> fullBooleanConceptVocabulary;
	static Map<String, Double> fullMoveConceptVocabulary;

	//---------------------------------------------------------------------
	
	public static void main(final String[] args)
	{
		calculateVocabularies();
		
		// Use this code to compare specific games.
		//final List<String[]> gamesAndRulesetsToCompare = getSpecificGamesToCompare();
		//recordAllComparisonDistances(gamesAndRulesetsToCompare.get(0), gamesAndRulesetsToCompare.get(1));
		
		// Use this code to compare all games.
		final List<String[]> gamesAndRulesetsToCompare = GameLoader.allAnalysisGameRulesetNames();
		final List<String> allGameNames = new ArrayList<>();
		final List<String> allRulesetNames = new ArrayList<>();
		for (final String[] allGamesToComare : gamesAndRulesetsToCompare)
		{
			allGameNames.add(allGamesToComare[0]);
			allRulesetNames.add(allGamesToComare[1]);
		}
		recordAllComparisonDistances(allGameNames.toArray(new String[0]), allRulesetNames.toArray(new String[0]));
	}
	
	//---------------------------------------------------------------------
	
	/**
	 * Record distances for each game/ruleset comparison.
	 */
	private static void recordAllComparisonDistances(final String[] gamesToCompare, final String[] rulesetsToCompare)
	{
		final List<Map<String, Map<String, Double>>> allGameDistances = new ArrayList<>();
		
		for (int i = 0; i < gamesToCompare.length; i++)
		{			
			final Map<String, Map<String, Double>> gameDistances = new HashMap<>();
			
			for (int j = 0; j < gamesToCompare.length; j++)
			{
				gameDistances.put
				(
					gamesToCompare[j] + "_" + rulesetsToCompare[j], 
					compareTwoGames(GameLoader.loadGameFromName(gamesToCompare[i], rulesetsToCompare[i]), GameLoader.loadGameFromName(gamesToCompare[j], rulesetsToCompare[j]))
				);
			}
			
			allGameDistances.add(gameDistances);
		}
		
		storeByGameResults(gamesToCompare, rulesetsToCompare, allGameDistances);
		storeByMetricResults(gamesToCompare, rulesetsToCompare, allGameDistances);
	}
	
	//---------------------------------------------------------------------
	
	/**
	 * Stores the distance results in a set of .csv files, with each file representing a single game and its distance to all other games.
	 * @param gamesToCompare
	 * @param rulesetsToCompare
	 * @param allGameDistances
	 */
	@SuppressWarnings("unchecked")
	private static void storeByGameResults(final String[] gamesToCompare, final String[] rulesetsToCompare, final List<Map<String, Map<String, Double>>> allGameDistances)
	{
		for (int i = 0; i < gamesToCompare.length; i++)
		{
			// Create output file.
			final File outputFile = new File(outputPath + "output/byGame/" +
											 gamesToCompare[i].split("\\/")[gamesToCompare[i].split("\\/").length-1] + "_" + 
											 rulesetsToCompare[i].split("\\/")[rulesetsToCompare[i].split("\\/").length-1] + ".csv");
			try
			{
				outputFile.createNewFile();
			}
			catch (final IOException e1)
			{
				e1.printStackTrace();
			} 
			
			// Store distances in output file.
			try (final FileWriter myWriter = new FileWriter(outputFile))
			{
				// Write the top row of the csv
				String topRow = "GameName,Id";
				final List<String> distanceNames = new ArrayList<>(((Map<String, Double>) allGameDistances.get(i).values().toArray()[0]).keySet());
				for (final String distance : distanceNames)
					topRow += "," + distance;
				myWriter.write(topRow + "\n");
				
				// Store all distances for this ruleset.
				for (final Map.Entry<String, Map<String, Double>> gameEntry : allGameDistances.get(i).entrySet()) 
				{
				    final String gameName = gameEntry.getKey();

				    // Get corresponding ruleset Id.
				    final String[] nameArray = gameName.split("_")[0].split("/");
				    final String formattedGameName = nameArray[nameArray.length-1].substring(0,nameArray[nameArray.length-1].length()-4);
				    String formattedRulesetName = "";
				    if (gameName.split("_").length > 1)
				    	formattedRulesetName = gameName.split("_")[1];
				    final int rulesetId = DatabaseInformation.getRulesetId(formattedGameName, formattedRulesetName);
				    
				    final List<String> row =  new ArrayList<>(distanceNames);
				    for (final Map.Entry<String, Double> distanceEntry : gameEntry.getValue().entrySet()) 
					{
				    	final String distanceMetric = distanceEntry.getKey();
				    	final Double distaneValue = distanceEntry.getValue();
				    	
				    	row.set(row.indexOf(distanceMetric), String.valueOf(distaneValue));
					}
				    
				    myWriter.write(gameName + "," + rulesetId + "," + String.join(",", row) + "\n");
				}
				
				myWriter.close();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	//---------------------------------------------------------------------
	
	/**
	 * Stores the distance results in a set of .csv files, with each file representing a single distance metric for all game-distance pairs.
	 * @param gamesToCompare
	 * @param rulesetsToCompare
	 * @param allGameDistances
	 */
	@SuppressWarnings("unchecked")
	private static void storeByMetricResults(final String[] gamesToCompare, final String[] rulesetsToCompare, final List<Map<String, Map<String, Double>>> allGameDistances)
	{
		final List<String> distanceNames = new ArrayList<>(((Map<String, Double>) allGameDistances.get(0).values().toArray()[0]).keySet());
		
		for (int i = 0; i < distanceNames.size(); i++)
		{
			// Create output file.
			final File outputFile = new File(outputPath + "output/byMetric/" + distanceNames.get(i) + ".csv");
			try
			{
				outputFile.createNewFile();
			}
			catch (final IOException e1)
			{
				e1.printStackTrace();
			} 
			
			// Store distances in output file.
			try (final FileWriter myWriter = new FileWriter(outputFile))
			{
				final List<String> allRulesetIds = new ArrayList<>();
				
				// Write the top row of the file
				String topRow = "Id";
				for (final Map.Entry<String, Map<String, Double>> gameEntry : allGameDistances.get(0).entrySet()) 
				{
					final String gameName = gameEntry.getKey();
				    
				    // Get corresponding ruleset Id.
				    final String[] nameArray = gameName.split("_")[0].split("/");
				    final String formattedGameName = nameArray[nameArray.length-1].substring(0,nameArray[nameArray.length-1].length()-4);
				    String formattedRulesetName = "";
				    if (gameName.split("_").length > 1)
				    	formattedRulesetName = gameName.split("_")[1];
				    final int rulesetId = DatabaseInformation.getRulesetId(formattedGameName, formattedRulesetName);
				    allRulesetIds.add(String.valueOf(rulesetId));
				    
				    topRow += "," + rulesetId;
				}
				myWriter.write(topRow + "\n");

				for (int j = 0; j < gamesToCompare.length; j++)
				{
					String row = allRulesetIds.get(j);
					
					// Store all distances for this ruleset.
					for (final Map.Entry<String, Map<String, Double>> gameEntry1 : allGameDistances.get(j).entrySet()) 
						row += "," + gameEntry1.getValue().get(distanceNames.get(i));
					
					myWriter.write(row + "\n");
				}

				myWriter.close();
			}
			catch (final IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	//---------------------------------------------------------------------
	
	/**
	 * Calculate dataset vocabularies for TFIDF measures.
	 */
	private static void calculateVocabularies()
	{
		fullLudemeVocabulary = DistanceUtils.fullVocabulary(ludemeDataset, "ludemeDataset", overrideStoredVocabularies);
		System.out.println("ludemeVocabulary recorded");
		
		fullBooleanConceptVocabulary = DistanceUtils.fullVocabulary(booleanConceptDataset, "booleanConceptDataset", overrideStoredVocabularies);
		System.out.println("booleanConceptVocabulary recorded");
		
		fullMoveConceptVocabulary = DistanceUtils.fullVocabulary(moveConceptDataset, "moveConceptDataset", overrideStoredVocabularies);
		System.out.println("moveConceptVocabulary recorded");
	}
	
	//---------------------------------------------------------------------
	
	/**
	 * Specific games/rulesets to compare.
	 * @return List of two String arrays, for all game and ruleset names to compare.
	 */
	@SuppressWarnings("unused")
	private static List<String[]> getSpecificGamesToCompare()
	{			
		final String[] gamesToCompare = 
			{
				"/lud/board/hunt/Haretavl.lud",
				"/lud/board/hunt/Jeu Militaire.lud",
				"/lud/board/hunt/Jeu Militaire.lud",
				"/lud/board/hunt/Hund efter Hare (Thy).lud",
				"/lud/board/hunt/Hund efter Hare (Vendsyssel).lud",
				"/lud/board/hunt/Hyvn aetter Hare.lud",
				"/lud/board/hunt/Janes Soppi.lud",
				"/lud/board/space/blocking/Janes Soppi (Symmetrical).lud",
				"/lud/board/hunt/Gioco dell'Orso.lud",
				"/lud/board/hunt/La Liebre Perseguida.lud",
				"/lud/board/hunt/Neg Tugal Tuux.lud",
				"/lud/board/hunt/Uxrijn Ever.lud",
			};

		final String[] rulesetsToCompare = 
			{
				"",
				"Ruleset/Lucas (Described)",
				"Ruleset/Gardner (Suggested)",
				"",
				"",
				"",
				"",
				"",
				"",
				"",
				"",
				"",
			};
		
		final List<String[]> gamesAndRulesetsToCompare = new ArrayList<>();
		gamesAndRulesetsToCompare.add(gamesToCompare);
		gamesAndRulesetsToCompare.add(rulesetsToCompare);
		return gamesAndRulesetsToCompare;
	}
	
	//---------------------------------------------------------------------
	
	/**
	 * Compares gameA and gameB across all distance measures
	 * @param gameA
	 * @param gameB
	 * @return Map of distance metric names and values.
	 */
	public static Map<String, Double> compareTwoGames(final Game gameA, final Game gameB)
	{
		final Map<String, Double> allDistances = new HashMap<>();
		
		ludemeDataset.getTree(gameA);
		System.out.println("\n" + gameA.name() + " v.s. " + gameB.name());
		
		//---------------------------------------------------------------------
		// Store the default vocabularies for each dataset.
		
		final Map<String, Double> defaultLudemeVocabulary = DistanceUtils.defaultVocabulary(ludemeDataset, gameA, gameB);
		final Map<String, Double> defaultLudemeNGramVocabulary = DistanceUtils.defaultVocabulary(new NGramDataset(ludemeDataset, DistanceUtils.nGramLength), gameA, gameB);
		
		final Map<String, Double> defaultBooleanConceptVocabulary = DistanceUtils.defaultVocabulary(booleanConceptDataset, gameA, gameB);
		
		//final Map<String, Double> defaultMoveConceptVocabulary = DistanceUtils.defaultVocabulary(moveConceptDataset, gameA, gameB);
		//final Map<String, Double> defaultMoveConceptNGramVocabulary = DistanceUtils.defaultVocabulary(new NGramDataset(moveConceptDataset, DistanceUtils.nGramLength), gameA, gameB);
		
		//---------------------------------------------------------------------
		// JensenShannonDivergence
		
		final DistanceMetric jensenShannonDivergenceMetric = new JensenShannonDivergence();
		
		allDistances.put("JSD_ludeme", jensenShannonDivergenceMetric.distance(ludemeDataset, defaultLudemeVocabulary, gameA, gameB));
		allDistances.put("JSD_booleanConcept", jensenShannonDivergenceMetric.distance(booleanConceptDataset, defaultBooleanConceptVocabulary, gameA, gameB));
		//allDistances.put("JSD_moveConcept", jensenShannonDivergenceMetric.distance(moveConceptDataset, defaultMoveConceptVocabulary, gameA, gameB));
		
		allDistances.put("JSD_ludeme_ngram", jensenShannonDivergenceMetric.distance(new NGramDataset(ludemeDataset, DistanceUtils.nGramLength), defaultLudemeNGramVocabulary, gameA, gameB));
		//allDistances.put("JSD_moveCooncept_ngram", jensenShannonDivergenceMetric.distance(new NGramDataset(moveConceptDataset, DistanceUtils.nGramLength), defaultMoveConceptNGramVocabulary, gameA, gameB));
		
		allDistances.put("JSD_ludeme_TFIDF", jensenShannonDivergenceMetric.distance(ludemeDataset, fullLudemeVocabulary, gameA, gameB));
		allDistances.put("JSD_booleanConcept_TFIDF", jensenShannonDivergenceMetric.distance(booleanConceptDataset, fullBooleanConceptVocabulary, gameA, gameB));
		//allDistances.put("JSD_moveConcept_TFIDF", jensenShannonDivergenceMetric.distance(moveConceptDataset, fullMoveConceptVocabulary, gameA, gameB));
		
		//---------------------------------------------------------------------
		// Cosine
		
		final DistanceMetric cosineMetric = new Cosine();
		
		allDistances.put("Cosine_ludeme", cosineMetric.distance(ludemeDataset, defaultLudemeVocabulary, gameA, gameB));
		allDistances.put("Cosine_booleanConcept", cosineMetric.distance(booleanConceptDataset, defaultBooleanConceptVocabulary, gameA, gameB));
		//allDistances.put("Cosine_moveConcept", cosineMetric.distance(moveConceptDataset, defaultMoveConceptVocabulary, gameA, gameB));
		
		allDistances.put("Cosine_ludeme_ngram", cosineMetric.distance(new NGramDataset(ludemeDataset, DistanceUtils.nGramLength), defaultLudemeNGramVocabulary, gameA, gameB));
		//allDistances.put("Cosine_moveCooncept_ngram", cosineMetric.distance(new NGramDataset(moveConceptDataset, DistanceUtils.nGramLength), defaultMoveConceptNGramVocabulary, gameA, gameB));
		
		allDistances.put("Cosine_ludeme_TFIDF", cosineMetric.distance(ludemeDataset, fullLudemeVocabulary, gameA, gameB));
		allDistances.put("Cosine_booleanConcept_TFIDF", cosineMetric.distance(booleanConceptDataset, fullBooleanConceptVocabulary, gameA, gameB));
		//allDistances.put("Cosine_moveConcept_TFIDF", cosineMetric.distance(moveConceptDataset, fullMoveConceptVocabulary, gameA, gameB));
		
		//---------------------------------------------------------------------
		// Jaccard
		
		final DistanceMetric jaccardMetric = new Jaccard();
		
		allDistances.put("Jaccard_ludeme", jaccardMetric.distance(ludemeDataset, defaultLudemeVocabulary, gameA, gameB));
		allDistances.put("Jaccard_booleanConcept", jaccardMetric.distance(booleanConceptDataset, defaultBooleanConceptVocabulary, gameA, gameB));
		//allDistances.put("Jaccard_moveConcept", jaccardMetric.distance(moveConceptDataset, defaultMoveConceptVocabulary, gameA, gameB));
		
		allDistances.put("Jaccard_ludeme_ngram", jaccardMetric.distance(new NGramDataset(ludemeDataset, DistanceUtils.nGramLength), defaultLudemeNGramVocabulary, gameA, gameB));
		//allDistances.put("Jaccard_moveCooncept_ngram", jaccardMetric.distance(new NGramDataset(moveConceptDataset, DistanceUtils.nGramLength), defaultMoveConceptNGramVocabulary, gameA, gameB));
		
		allDistances.put("Jaccard_ludeme_TFIDF", jaccardMetric.distance(ludemeDataset, fullLudemeVocabulary, gameA, gameB));
		allDistances.put("Jaccard_booleanConcept_TFIDF", jaccardMetric.distance(booleanConceptDataset, fullBooleanConceptVocabulary, gameA, gameB));
		//allDistances.put("Jaccard_moveConcept_TFIDF", jaccardMetric.distance(moveConceptDataset, fullMoveConceptVocabulary, gameA, gameB));
		
		//---------------------------------------------------------------------
		// Levenshtein
		
		final DistanceMetric levenshteinMetric = new Levenshtein();
		
		allDistances.put("Levenshtein_ludeme", levenshteinMetric.distance(ludemeDataset, null, gameA, gameB));
		//allDistances.put("Levenshtein_moveConcept", levenshteinMetric.distance(moveConceptDataset, null, gameA, gameB));
		
		//---------------------------------------------------------------------
		// Local Alignment
		
		final DistanceMetric localAlignmentMetric = new LocalAlignment();
		
		allDistances.put("LocalAlignment_ludeme", localAlignmentMetric.distance(ludemeDataset, null, gameA, gameB));
		//allDistances.put("LocalAlignment_moveConcept", localAlignmentMetric.distance(moveConceptDataset, null, gameA, gameB));
		
		//---------------------------------------------------------------------
		// Repeated Local Alignment
		
		final DistanceMetric repeatedLocalAlignmentMetric = new RepeatedLocalAlignment();
		
		allDistances.put("RepeatedLocalAlignment_ludeme", repeatedLocalAlignmentMetric.distance(ludemeDataset, null, gameA, gameB));
		//allDistances.put("RepeatedLocalAlignment_moveConcept", repeatedLocalAlignmentMetric.distance(moveConceptDataset, null, gameA, gameB));
		
		//---------------------------------------------------------------------
		// Global Alignment
		
		final DistanceMetric globalAlignmentMetric = new GlobalAlignment();
		
		allDistances.put("GlobalAlignment_ludeme", globalAlignmentMetric.distance(ludemeDataset, null, gameA, gameB));
		//allDistances.put("GlobalAlignment_moveConcept", globalAlignmentMetric.distance(moveConceptDataset, null, gameA, gameB));
		
		//---------------------------------------------------------------------
		// Zhang Shasha
		
		final DistanceMetric zhangShashaMetric = new ZhangShasha();
		
		allDistances.put("ZhangShasha_ludeme", zhangShashaMetric.distance(ludemeDataset, defaultLudemeVocabulary, gameA, gameB));
		
		//---------------------------------------------------------------------
		// Apted
		
		final DistanceMetric aptedMetric = new Apted();
		
		allDistances.put("Apted_ludeme", aptedMetric.distance(ludemeDataset, defaultLudemeVocabulary, gameA, gameB));
		
		//---------------------------------------------------------------------
		
		return allDistances;
	}
	
	//---------------------------------------------------------------------
	
}
