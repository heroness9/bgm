public class RandarCoordFinder
{
    public static final long X_MULT = 341873128712L;
    public static final long Z_MULT = 132897987541L;

    public static final long Z_MULT_INV = 211541297333629L;

    public static final int MANSION_SALT = 10387319;
    public static final int MANSION_SPACING = 80;

    public static final int CITY_SALT = 10387313;
    public static final int CITY_SPACING = 20;

    // the last seed we processed
    public long lastSeed = -1;
    // a mapping of seed -> x,z that is updated everytime we get a hit
    public final HashMap<Long, Long> hitCache = new HashMap<>();

    // set this according to the server's seed
    public long worldSeed;

    // change these if you need to use different structures
    public int salt = MANSION_SALT;
    public int spacing = MANSION_SPACING;

    public RandarCoordFinder(long worldSeed)
    {
        this.worldSeed = worldSeed;
    }

    // a simple class that extends java.util.Random and provides some extra methods and constants we need
    public static class RandarRandom extends Random
    {
        public static final long MULT = 0x5DEECE66DL;
        public static final long ADDEND = 0xBL;
        public static final long MASK = (1L << 48) - 1;

        public static final long MULT_INV = 0xDFE05BCB1365L;

        public long seed;

        public RandarRandom(long seed)
        {
            this.seed = seed;
        }

        @Override
        public void setSeed(long seed)
        {
            this.seed = seed;
        }

        @Override
        public int next(int bits)
        {
            seed = (seed * MULT + ADDEND) & MASK;
            return (int)(seed >> 48 - bits);
        }

        public int prevInt()
        {
            seed = ((seed - ADDEND) * MULT_INV) & MASK;
            return (int)(seed >> 16);
        }
    }

    public enum FindType
    {
        HIT,
        SKIP,
        FAIL;
    }

    public record FindResult(FindType type, int xCoord, int zCoord, int steps)
    {
    }

    public FindResult findCoordsSeed(long seed, int maxSteps)
    {
        seed &= RandarRandom.MASK;

        // remember and update lastSeed
        long last = lastSeed;
        lastSeed = seed;

        RandarRandom random = new RandarRandom(seed);

        // first pass - this is meant to be quick
        for (int i = 0; i < maxSteps + 100000; i++)
        {
            if (random.seed == last && i > 0)
            {
                // we encountered the last processed seed while stepping back, skip
                return new FindResult(FindType.SKIP, 0, 0, i);
            }
            else
            {
                Long hashValue = hitCache.get(random.seed);
                if (hashValue != null)
                {
                    // we found a hit in our cache
                    int xCoord = (int)((hashValue >> 32) & 0xFFFFFFFF);
                    int zCoord = (int)(hashValue & 0xFFFFFFFF);
                    cacheNearby(xCoord, zCoord, 8);
                    return new FindResult(FindType.HIT, xCoord, zCoord, i);
                }
            }

            random.prevInt();
        }

        random.seed = seed;

        // second pass - this is slow and should only happen if the first pass fails
        for (int i = 0; i < maxSteps; i++)
        {
            // undo worldSeed and salt
            long seedValue = (random.seed ^ RandarRandom.MULT) - worldSeed -
                (long)salt;

            Coords coords = findCoords(seedValue, 1875000 / spacing + 8);
            if (coords != null)
            {
                // we found a hit
                cacheNearby(coords.x, coords.z, 8);
                return new FindResult(FindType.HIT, coords.x, coords.z, i);
            }
            random.prevInt();
        }

        // we could not find anything
        return new FindResult(FindType.FAIL, 0, 0, -1);
    }

    public static long getRandomSeed(int x, int z, int salt, long seed)
    {
        return ((long)x * X_MULT + (long)z * Z_MULT) + seed + (long)salt;
    }

    private void cacheNearby(int x, int z, int radius)
    {
        for (int xOff = -radius; xOff <= radius; xOff++)
        {
            for (int zOff = -radius; zOff <= radius; zOff++)
            {
                int cacheX = x + xOff;
                int cacheZ = z + zOff;
                long cacheSeed = (getRandomSeed(cacheX, cacheZ, salt,
                    worldSeed) ^ RandarRandom.MULT) & RandarRandom.MASK;
                hitCache.put(cacheSeed, (long)cacheX << 32 | cacheZ &
                        0xFFFFFFFFL);
            }
        }
    }

    public record Coords(int x, int z)
    {
    }

    public static Coords findCoords(long value, int distance)
    {
        value &= RandarRandom.MASK;

        for (int x = -distance; x <= distance; x++)
        {
            long testValue = (value - X_MULT * x) & RandarRandom.MASK;
            long z = (testValue * Z_MULT_INV) << 16 >> 16;
            if (Math.abs(z) <= distance)
            {
                return new Coords(x, (int)z);
            }
        }

        return null;
    }
}
