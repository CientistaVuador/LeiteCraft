package cientistavuador.leitecraft;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cien
 */
public class Blocks {
    
    public static void init() {
        
    }
    
    private static final Map<Integer, Block> blockIds = new HashMap<>();
    
    private static void register(Block block) {
        blockIds.put(block.getId(), block);
    }
    
    private static AtlasTexture texture(String name) {
        return Atlas.getInstance().getTexture(name);
    }
    
    public static final Block AIR = null;
    public static final Block DIRT = new Block(1, texture("dirt"));
    public static final Block GRASS = new Block(2, texture("grass_top"), texture("grass_side"), texture("dirt"));
    public static final Block STONE = new Block(3, texture("stone"));
    public static final Block SAND = new Block(4, texture("sand"));
    public static final Block BEDROCK = new Block(5, texture("bedrock"));
    public static final Block GLASS = new Block(6, texture("glass"));
    public static final Block WOOD = new Block(7, texture("wood_topbottom"), texture("wood_side"));
    public static final Block PLANKS = new Block(8, texture("planks"));
    public static final Block FOLIAGE = new Block(9, texture("foliage"));
    public static final Block COAL_ORE = new Block(10, texture("coal_ore"));
    public static final Block IRON_ORE = new Block(11, texture("iron_ore"));
    public static final Block GLASS_RED = new Block(12, texture("glass_red"));
    public static final Block GLASS_GREEN = new Block(13, texture("glass_green"));
    public static final Block GLASS_BLUE = new Block(14, texture("glass_blue"));
    public static final Block WATER = new Block(15,
            texture("water_side_start"), texture("water_side_start"),
            texture("water_top_start"), texture("water_top_start"),
            texture("water_side_start"), texture("water_side_start"),
            
            texture("water_side_end"), texture("water_side_end"),
            texture("water_top_end"), texture("water_top_end"),
            texture("water_side_end"), texture("water_side_end")
    );
    public static final Block LAVA = new Block(16,
            texture("lava_side_start"), texture("lava_side_start"),
            texture("lava_top_start"), texture("lava_top_start"),
            texture("lava_side_start"), texture("lava_side_start"),
            
            texture("lava_side_end"), texture("lava_side_end"),
            texture("lava_top_end"), texture("lava_top_end"),
            texture("lava_side_end"), texture("lava_side_end")
    );
    public static final BillboardBlock GRASS_LEAVES = new BillboardBlock(17, texture("grass_leaves"));
    public static final Block LANTERN = new Block(18, texture("lantern"));
    
    static {
        GLASS.setTransparent(true);
        
        GLASS_RED.setTransparent(true);
        GLASS_RED.setAlphaEnabled(true);
        
        GLASS_GREEN.setTransparent(true);
        GLASS_GREEN.setAlphaEnabled(true);
        
        GLASS_BLUE.setTransparent(true);
        GLASS_BLUE.setAlphaEnabled(true);
        
        WATER.setTransparent(true);
        WATER.setAlphaEnabled(true);
        WATER.setCollision(false);
        WATER.setLiquid(true);
        WATER.setOverlayTexture(texture("water_top_start"));
        
        GRASS_LEAVES.setTransparent(true);
        GRASS_LEAVES.setCollision(false);
        
        LAVA.setLightEmission(Chunk.MAX_LIGHT_LEVEL);
        LAVA.setTransparent(true);
        LAVA.setCollision(false);
        LAVA.setLiquid(true);
        LAVA.setOverlayTexture(texture("lava_top_start"));
        
        LANTERN.setLightEmission(Chunk.MAX_LIGHT_LEVEL);
    }
    
    private static final Block[] blocks;
    
    static {
        register(DIRT);
        register(GRASS);
        register(STONE);
        register(SAND);
        register(BEDROCK);
        register(GLASS);
        register(WOOD);
        register(PLANKS);
        register(FOLIAGE);
        register(COAL_ORE);
        register(IRON_ORE);
        register(GLASS_RED);
        register(GLASS_GREEN);
        register(GLASS_BLUE);
        register(WATER);
        register(LAVA);
        register(GRASS_LEAVES);
        register(LANTERN);
        
        List<Block> blockList = new ArrayList<>();
        for (Map.Entry<Integer, Block> e:blockIds.entrySet()) {
            blockList.add(e.getValue());
        }
        
        blocks = blockList.toArray(Block[]::new);
    }
    
    public static Block getBlock(int id) {
        return blockIds.get(id);
    }
    
    public static Block[] getBlocks() {
        return blocks;
    }
    
    private Blocks() {
        
    }
}
