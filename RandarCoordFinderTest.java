import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.util.HashMap;

public class RandarCoordFinderTest {
    private RandarCoordFinder finder;
    private static final long TEST_WORLD_SEED = 123456789L;

    @BeforeEach
    public void setUp() {
        finder = new RandarCoordFinder(TEST_WORLD_SEED);
    }

    // ============ CONSTRUCTOR TESTS ============
    @Test
    public void testConstructorInitialization() {
        RandarCoordFinder f = new RandarCoordFinder(TEST_WORLD_SEED);
        assertEquals(TEST_WORLD_SEED, f.worldSeed);
        assertEquals(-1, f.lastSeed);
        assertTrue(f.hitCache.isEmpty());
        assertEquals(RandarCoordFinder.MANSION_SALT, f.salt);
        assertEquals(RandarCoordFinder.MANSION_SPACING, f.spacing);
    }

    // ============ RANDAR RANDOM TESTS ============
    @Test
    public void testRandarRandomNext() {
        RandarCoordFinder.RandarRandom random = new RandarCoordFinder.RandarRandom(12345L);
        long initialSeed = random.seed;
        
        int result = random.next(16);
        
        assertNotEquals(initialSeed, random.seed);
        assertTrue(result >= 0 && result < (1 << 16));
    }

    @Test
    public void testRandarRandomPrevInt() {
        RandarCoordFinder.RandarRandom random = new RandarCoordFinder.RandarRandom(12345L);
        long originalSeed = random.seed;
        
        random.next(32);
        long afterNext = random.seed;
        
        int prevResult = random.prevInt();
        
        assertNotEquals(afterNext, random.seed);
    }

    @Test
    public void testRandarRandomSetSeed() {
        RandarCoordFinder.RandarRandom random = new RandarCoordFinder.RandarRandom(100L);
        random.setSeed(999L);
        assertEquals(999L, random.seed);
    }

    @Test
    public void testRandarRandomMaskApplication() {
        long largeSeed = Long.MAX_VALUE;
        RandarCoordFinder.RandarRandom random = new RandarCoordFinder.RandarRandom(largeSeed);
        
        assertEquals(largeSeed & RandarCoordFinder.RandarRandom.MASK, random.seed);
    }

    // ============ GET RANDOM SEED TESTS ============
    @Test
    public void testGetRandomSeed() {
        long result = RandarCoordFinder.getRandomSeed(10, 20, 100, TEST_WORLD_SEED);
        
        long expected = (10L * RandarCoordFinder.X_MULT + 
                        20L * RandarCoordFinder.Z_MULT) + 
                        TEST_WORLD_SEED + 100;
        
        assertEquals(expected, result);
    }

    @Test
    public void testGetRandomSeedZeroCoords() {
        long result = RandarCoordFinder.getRandomSeed(0, 0, 0, 0);
        assertEquals(0, result);
    }

    @Test
    public void testGetRandomSeedNegativeCoords() {
        long result = RandarCoordFinder.getRandomSeed(-10, -20, 50, TEST_WORLD_SEED);
        
        long expected = (-10L * RandarCoordFinder.X_MULT + 
                        -20L * RandarCoordFinder.Z_MULT) + 
                        TEST_WORLD_SEED + 50;
        
        assertEquals(expected, result);
    }

    // ============ FIND COORDS TESTS ============
    @Test
    public void testFindCoordsValid() {
        long seed = RandarCoordFinder.getRandomSeed(50, 75, 0, 0);
        
        RandarCoordFinder.Coords coords = RandarCoordFinder.findCoords(seed, 1000);
        assertNotNull(coords);
        assertEquals(50, coords.x);
        assertEquals(75, coords.z);
    }

    @Test
    public void testFindCoordsSmallDistance() {
        long seed = RandarCoordFinder.getRandomSeed(5, 10, 0, 0);
        
        RandarCoordFinder.Coords coords = RandarCoordFinder.findCoords(seed, 5);
        if (coords != null) {
            assertTrue(Math.abs(coords.x) <= 5);
            assertTrue(Math.abs(coords.z) <= 5);
        }
    }

    @Test
    public void testFindCoordsLargeDistance() {
        long seed = RandarCoordFinder.getRandomSeed(1000, 2000, 0, 0);
        
        RandarCoordFinder.Coords coords = RandarCoordFinder.findCoords(seed, 5000);
        assertNotNull(coords);
    }

    @Test
    public void testFindCoordsNegativeCoords() {
        long seed = RandarCoordFinder.getRandomSeed(-100, -200, 0, 0);
        
        RandarCoordFinder.Coords coords = RandarCoordFinder.findCoords(seed, 1000);
        assertNotNull(coords);
        assertEquals(-100, coords.x);
        assertEquals(-200, coords.z);
    }

    // ============ CACHE NEARBY TESTS ============
    @Test
    public void testCacheNearby() {
        int initialSize = finder.hitCache.size();
        
        finder.cacheNearby(50, 75, 2);
        
        assertEquals(initialSize + 25, finder.hitCache.size());
    }

    @Test
    public void testCacheNearbyLargeRadius() {
        finder.cacheNearby(0, 0, 5);
        
        assertEquals(121, finder.hitCache.size());
    }

    @Test
    public void testCacheNearbyValues() {
        finder.cacheNearby(10, 20, 1);
        
        long centerSeed = (RandarCoordFinder.getRandomSeed(10, 20, finder.salt, 
                          TEST_WORLD_SEED) ^ RandarCoordFinder.RandarRandom.MULT) & 
                          RandarCoordFinder.RandarRandom.MASK;
        
        assertTrue(finder.hitCache.containsKey(centerSeed));
        Long cachedValue = finder.hitCache.get(centerSeed);
        
        int cachedX = (int)((cachedValue >> 32) & 0xFFFFFFFF);
        int cachedZ = (int)(cachedValue & 0xFFFFFFFFL);
        
        assertEquals(10, cachedX);
        assertEquals(20, cachedZ);
    }

    // ============ FIND COORDS SEED TESTS ============
    @Test
    public void testFindCoordsSeedHIT() {
        finder.cacheNearby(100, 200, 3);
        
        long seed = RandarCoordFinder.getRandomSeed(100, 200, finder.salt, TEST_WORLD_SEED);
        
        RandarCoordFinder.FindResult result = finder.findCoordsSeed(seed, 1000);
        
        assertEquals(RandarCoordFinder.FindType.HIT, result.type);
        assertEquals(100, result.xCoord);
        assertEquals(200, result.zCoord);
    }

    @Test
    public void testFindCoordsSeedFAIL() {
        RandarCoordFinder.FindResult result = finder.findCoordsSeed(0xABCDEF, 100);
        
        assertEquals(RandarCoordFinder.FindType.FAIL, result.type);
        assertEquals(-1, result.steps);
    }

    @Test
    public void testFindCoordsSeedUpdateLastSeed() {
        long seed1 = 123L;
        long seed2 = 456L;
        
        finder.findCoordsSeed(seed1, 100);
        assertEquals(seed1 & RandarCoordFinder.RandarRandom.MASK, finder.lastSeed);
        
        finder.findCoordsSeed(seed2, 100);
        assertEquals(seed2 & RandarCoordFinder.RandarRandom.MASK, finder.lastSeed);
    }

    @Test
    public void testFindCoordsSeedMaxStepsRespected() {
        int maxSteps = 50;
        RandarCoordFinder.FindResult result = finder.findCoordsSeed(0xDEADBEEF, maxSteps);
        
        if (result.type == RandarCoordFinder.FindType.FAIL) {
            assertEquals(-1, result.steps);
        }
    }

    // ============ FIND TYPE ENUM TESTS ============
    @Test
    public void testFindTypeValues() {
        assertTrue(RandarCoordFinder.FindType.HIT != null);
        assertTrue(RandarCoordFinder.FindType.SKIP != null);
        assertTrue(RandarCoordFinder.FindType.FAIL != null);
    }

    // ============ FIND RESULT RECORD TESTS ============
    @Test
    public void testFindResultRecord() {
        RandarCoordFinder.FindResult result = 
            new RandarCoordFinder.FindResult(RandarCoordFinder.FindType.HIT, 10, 20, 5);
        
        assertEquals(RandarCoordFinder.FindType.HIT, result.type);
        assertEquals(10, result.xCoord);
        assertEquals(20, result.zCoord);
        assertEquals(5, result.steps);
    }

    // ============ CONSTANTS TESTS ============
    @Test
    public void testConstants() {
        assertEquals(341873128712L, RandarCoordFinder.X_MULT);
        assertEquals(132897987541L, RandarCoordFinder.Z_MULT);
        assertEquals(211541297333629L, RandarCoordFinder.Z_MULT_INV);
        assertEquals(10387319, RandarCoordFinder.MANSION_SALT);
        assertEquals(80, RandarCoordFinder.MANSION_SPACING);
        assertEquals(10387313, RandarCoordFinder.CITY_SALT);
        assertEquals(20, RandarCoordFinder.CITY_SPACING);
    }
}