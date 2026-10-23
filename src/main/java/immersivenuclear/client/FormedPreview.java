package immersivenuclear.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.item.ItemStack;

/**
 * The formed machine for IMultiblock.renderFormedStructure (IE's manual, Immersive Petroleum's projector): our 3D item
 * model (the whole machine, as the blockstate's inventory variant) drawn where the blocks would be. Those screens
 * draw block structure[h][l][w] at (l, h, w), so l (template dz) runs along x and w (template dx) along z; a quarter
 * turn puts the model the same way round without mirroring it.
 */
public class FormedPreview
{
	/**
	 * @param masterX template position of the master block, from the structure's lowest corner (dx - MIN_DX, ...)
	 * @param width   the structure's size along dx (structure[0][0].length)
	 */
	public static void render(ItemStack item, int masterX, int masterY, int masterZ, int width)
	{
		Minecraft mc = Minecraft.getMinecraft();
		IBakedModel model = mc.getRenderItem().getItemModelMesher().getItemModel(item);
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, 0, width);
		GlStateManager.rotate(90, 0, 1, 0);
		GlStateManager.translate(masterX, masterY, masterZ);
		// renderItem centres the model on the block (-0.5), the OBJ is in the master block's own space
		GlStateManager.translate(0.5, 0.5, 0.5);
		RenderHelper.enableStandardItemLighting();
		mc.getRenderItem().renderItem(item, model);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.popMatrix();
	}
}
