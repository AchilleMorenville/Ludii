package game.functions.booleans.deductionPuzzle.is.regionResult;

import java.util.Arrays;
import java.util.BitSet;

import annotations.Hide;
import annotations.Opt;
import game.Game;
import game.equipment.other.Regions;
import game.functions.booleans.BaseBooleanFunction;
import game.functions.ints.IntConstant;
import game.functions.ints.IntFunction;
import game.functions.region.RegionFunction;
import game.types.board.RegionTypeStatic;
import game.types.board.SiteType;
import game.types.state.GameType;
import main.StringRoutines;
import other.concept.Concept;
import other.context.Context;
import other.state.container.ContainerState;

/**
 * Returns true if the count of a region is equal to the result.
 * 
 * @author Eric.Piette
 * 
 * @remarks This works only for deduction puzzles.
 */
@Hide
public class IsCount extends BaseBooleanFunction
{
	private static final long serialVersionUID = 1L;

	//-------------------------------------------------------------------------

	/** Which region. */
	private final RegionFunction region;
	
	/** What. */
	private final IntFunction whatFn;

	/** Which result. */
	private final IntFunction resultFn;
	
	/** The name of the region to check.. */
	private final String name;

	/** Which type. */
	private final SiteType type;
	
	//-------------------------------------------------------------------------

	/**
	 * @param type   The graph element of the region [Default SiteType of the board].
	 * @param region The region to count.
	 * @param what   The index of the piece to count [1].
	 * @param result The result to check.
	 */
	public IsCount
	(
		@Opt final SiteType       type,
		@Opt final RegionFunction region,
		@Opt final IntFunction    what,
		@Opt final String         nameRegion,
			 final IntFunction    result
	)
	{
		this.region = region;
		whatFn = (what == null) ? new IntConstant(1) : what;
		
		if(region != null)
			regionConstraint = region;
		else
			areaConstraint = RegionTypeStatic.Regions;
		
		resultFn = result;
		this.type = type;
		
		name = (nameRegion == null) ? "" : nameRegion;
	}
	
	//--------------------------------------------------------------------------

	@Override
	public boolean eval(Context context)
	{
		
		final ContainerState ps = context.state().containerStates()[0];
		final int what = whatFn.eval(context);
		final SiteType realType = (type == null) ? context.board().defaultSite() : type;
		
		if (region != null)
		{
			final int result = resultFn.eval(context);
			final int[] sites = region.eval(context).sites();
			boolean allAssigned = true;
			int currentCount = 0;
		
			for (final int site : sites) {
				if (ps.isResolved(site, realType)) 
				{
					final int whatSite = ps.what(site, realType);
					if (whatSite == what)
						currentCount ++;
				}
				else
					allAssigned = false;
			}
		
			if ((allAssigned && currentCount != result) || (currentCount > result))
				return false;
		}
		else 
		{
			int result = resultFn.eval(context);
			final Regions[] regions = context.game().equipment().regions();
			
			Integer[][] regionHint;
			if (type == SiteType.Cell)
				regionHint = context.game().equipment().cellHints();
			else if (type == SiteType.Vertex)
				regionHint = context.game().equipment().vertexHints();
			else
				regionHint = context.game().equipment().edgeHints();

			for (final Regions reg : regions)
			{
				if (reg.name().contains(name)) {
					if (reg.regionTypes() != null)
					{
						final RegionTypeStatic[] areas = reg.regionTypes();
						for (final RegionTypeStatic area : areas)
						{
							final Integer[][] regionsList = reg.convertStaticRegionOnLocs(area, context);
							int indexRegion = 0;
							for (final Integer[] locs : regionsList)
							{
								if (resultFn.isHint())
								{
									context.setHint(Arrays.stream(regionHint[indexRegion]).mapToInt(Integer::intValue).toArray());
									result = resultFn.eval(context);
								}
								boolean allAssigned = true;
								int currentCount = 0;
								for (final Integer loc : locs)
								{
									if (ps.isResolved(loc.intValue(), type)) {
										final int whatSite = ps.what(loc.intValue(), realType);
										if (whatSite == what)
											currentCount ++;
									}
									else
										allAssigned = false;
								}
									
								if ((allAssigned && currentCount != result) || (currentCount > result))
									return false;
								indexRegion++;
							}
						}
					}
					else {
						boolean allAssigned = true;
						int currentCount = 0;
	
						if (reg.sites() != null)
							for (final Integer loc : reg.sites())
							{
								if (ps.isResolved(loc.intValue(), type)) {
									final int whatSite = ps.what(loc.intValue(), realType);
									if (whatSite == what)
										currentCount ++;
								}
								else
									allAssigned = false;
							}
							
						if ((allAssigned && currentCount != result) || (currentCount > result))
							return false;
					}
						
				}
			}
		}
		
		/*
		if (region == null)
			return false;

		final SiteType realType = (type == null) ? context.board().defaultSite() : type;

		final ContainerState ps = context.state().containerStates()[0];
		final int what = whatFn.eval(context);
		final int result = resultFn.eval(context);
		final int[] sites = region.eval(context).sites();
		
		boolean assigned = true;
		int currentCount = 0;
		
		for (final int site : sites)
		{
			if (ps.isResolved(site, realType))
			{
				final int whatSite = ps.what(site, realType);
				if (whatSite == what)
					currentCount++;
			}
			else
				assigned = false;
		}
			
		if ((assigned && currentCount != result) || (currentCount > result))
			return false;
		*/
		return true;
	}

	//-------------------------------------------------------------------------

	@Override
	public boolean isStatic()
	{
		return false;
	}
	
	@Override
	public void preprocess(final Game game)
	{
		region.preprocess(game);
		whatFn.preprocess(game);
		resultFn.preprocess(game);
	}

	@Override
	public long gameFlags(final Game game)
	{
		long gameFlags = GameType.DeductionPuzzle;

		gameFlags |= region.gameFlags(game);
		gameFlags |= whatFn.gameFlags(game);
		gameFlags |= resultFn.gameFlags(game);

		return gameFlags;
	}

	@Override
	public BitSet concepts(final Game game)
	{
		final BitSet concepts = new BitSet();
		concepts.or(super.concepts(game));
		concepts.set(Concept.DeductionPuzzle.id(), true);

		concepts.or(region.concepts(game));
		concepts.or(whatFn.concepts(game));
		concepts.or(resultFn.concepts(game));

		return concepts;
	}

	@Override
	public BitSet writesEvalContextRecursive()
	{
		final BitSet writeEvalContext = new BitSet();
		writeEvalContext.or(super.writesEvalContextRecursive());
		writeEvalContext.or(region.writesEvalContextRecursive());
		writeEvalContext.or(whatFn.writesEvalContextRecursive());
		writeEvalContext.or(resultFn.writesEvalContextRecursive());
		return writeEvalContext;
	}

	@Override
	public BitSet readsEvalContextRecursive()
	{
		final BitSet readEvalContext = new BitSet();
		readEvalContext.or(super.readsEvalContextRecursive());
		readEvalContext.or(region.readsEvalContextRecursive());
		readEvalContext.or(whatFn.readsEvalContextRecursive());
		readEvalContext.or(resultFn.readsEvalContextRecursive());
		return readEvalContext;
	}

	@Override
	public boolean missingRequirement(final Game game)
	{
		boolean missingRequirement = false;
		missingRequirement |= super.missingRequirement(game);
		missingRequirement |= region.missingRequirement(game);
		missingRequirement |= whatFn.missingRequirement(game);
		missingRequirement |= resultFn.missingRequirement(game);
		return missingRequirement;
	}

	@Override
	public boolean willCrash(final Game game)
	{
		boolean willCrash = false;
		if (game.players().count() != 1)
		{
			game.addCrashToReport("The ludeme (is Count ...) is used but the number of players is not 1.");
			willCrash = true;
		}
		willCrash |= super.willCrash(game);
		willCrash |= region.willCrash(game);
		willCrash |= whatFn.willCrash(game);
		willCrash |= resultFn.willCrash(game);
		return willCrash;
	}

	//-------------------------------------------------------------------------

	/**
	 * @return The region to count.
	 */
	public RegionFunction region() 
	{
		return region;
	}

	/**
	 * @return The result to check.
	 */
	public IntFunction result() 
	{
		return resultFn;
	}
	
	/**
	 * @return The piece to count.
	 */
	public IntFunction what() 
	{
		return whatFn;
	}
	
	//-------------------------------------------------------------------------

	@Override
	public String toString()
	{
		String str = "";
		str += "Count(" + region + ") = " + resultFn;
		return str;
	}
	
	//-------------------------------------------------------------------------
	
	@Override
	public String toEnglish(final Game game)
	{
		return "the number of " + whatFn.toEnglish(game) + StringRoutines.getPlural(whatFn.toEnglish(game)) + " in " + region.toEnglish(game) + " equals " + resultFn.toEnglish(game);
	}
	
	//-------------------------------------------------------------------------
}
