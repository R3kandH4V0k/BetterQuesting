package betterquesting.client.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import betterquesting.api.api.QuestingAPI;
import betterquesting.api.client.gui.GuiScreenThemed;
import betterquesting.api.client.gui.QuestLineButtonTree;
import betterquesting.api.client.gui.controls.GuiButtonQuestInstance;
import betterquesting.api.client.gui.controls.GuiButtonQuestLine;
import betterquesting.api.client.gui.controls.GuiButtonThemed;
import betterquesting.api.client.gui.lists.GuiScrollingButtons;
import betterquesting.api.client.gui.lists.GuiScrollingText;
import betterquesting.api.client.gui.misc.INeedsRefresh;
import betterquesting.api.questing.IQuestLine;
import betterquesting.client.gui.editors.GuiQuestLineEditorA;
import betterquesting.questing.QuestLineDatabase;
import betterquesting.storage.QuestSettings;

@SideOnly(Side.CLIENT)
public class GuiQuestLinesMain extends GuiScreenThemed implements INeedsRefresh
{
	/**
	 * Last opened quest screen from here
	 */
	public static GuiQuestInstance bookmarked;
	
	private List<Integer> lineIDs = new ArrayList<Integer>();
	
	private GuiButtonQuestLine selected;
	private GuiScrollingButtons qlBtnList;
	private GuiQuestLinesEmbedded qlGui;
	private GuiScrollingText qlDesc;
	
	public GuiQuestLinesMain(GuiScreen parent)
	{
		super(parent, I18n.format("betterquesting.title.quest_lines"));
	}
	
	@Override
	public void initGui()
	{
		super.initGui();
		
		bookmarked = null;
		lineIDs = QuestLineDatabase.INSTANCE.getAllKeys();
		
		if(QuestSettings.INSTANCE.canUserEdit(mc.player))
		{
			((GuiButton)this.buttonList.get(0)).xPosition = this.width/2 - 100;
			((GuiButton)this.buttonList.get(0)).width = 100;
		}
		
		GuiButtonThemed btnEdit = new GuiButtonThemed(1, this.width/2, this.guiTop + this.sizeY - 16, 100, 20, I18n.format("betterquesting.btn.edit"), true);
		btnEdit.enabled = btnEdit.visible = QuestSettings.INSTANCE.canUserEdit(mc.player);
		this.buttonList.add(btnEdit);
		
		GuiQuestLinesEmbedded oldGui = qlGui;
		qlGui = new GuiQuestLinesEmbedded(guiLeft + 174, guiTop + 32, sizeX - (32 + 150 + 8), sizeY - 64 - 32);
		qlDesc = new GuiScrollingText(mc, guiLeft + 174, guiTop + 32 + sizeY - 64 - 32, sizeX - (32 + 150 + 8), 48);
		qlBtnList = new GuiScrollingButtons(mc, guiLeft + 16, guiTop + 32, 150, sizeY - 48);
		
		boolean reset = true;
		
		for(int j = 0; j < lineIDs.size(); j++)
		{
			int lID = lineIDs.get(j);
			IQuestLine line = QuestLineDatabase.INSTANCE.getValue(lID);
			
			if(line == null)
			{
				continue;
			}
			
			GuiButtonQuestLine btnLine = new GuiButtonQuestLine(2, 0, 0, 142, 20, line);
			btnLine.enabled = line.size() <= 0 || QuestSettings.INSTANCE.canUserEdit(mc.player);
			
			if(selected != null && QuestLineDatabase.INSTANCE.getKey(selected.getQuestLine()) == lID)
			{
				reset = false;
				selected = btnLine;
			}
			
			if(!btnLine.enabled)
			{
				UUID playerID = QuestingAPI.getQuestingUUID(mc.player);
				
				for(GuiButtonQuestInstance p : btnLine.getButtonTree().getButtonTree())
				{
					if((p.getQuest().isComplete(playerID) || p.getQuest().isUnlocked(playerID)) && (selected == null || selected.getQuestLine() != line))
					{
						btnLine.enabled = true;
						break;
					}
				}
			}
			
			qlBtnList.addButtonRow(btnLine);
		}
		
		if(reset || selected == null)
		{
			selected = null;
		} else
		{
			qlDesc.SetText(I18n.format(selected.getQuestLine().getUnlocalisedDescription()));
			qlGui.setQuestLine(selected.getButtonTree(), true);
			selected.enabled = false;
		}
		
		if(oldGui != null) // Preserve old settings
		{
			qlGui.copySettings(oldGui);
			this.embedded.remove(oldGui);
		}
		
		this.embedded.add(qlGui);
		this.embedded.add(qlDesc);
		this.embedded.add(qlBtnList);
	}
	
	@Override
	public void refreshGui()
	{
		initGui();
	}
	
	@Override
	public void actionPerformed(GuiButton button)
	{
		super.actionPerformed(button);
		
		if(button.id == 1)
		{
			mc.displayGuiScreen(new GuiQuestLineEditorA(this));
			// Quest line editor
		} else if(button instanceof GuiButtonQuestLine)
		{
			if(selected != null)
			{
				selected.enabled = true;
			}
			
			button.enabled = false;
			
			selected = (GuiButtonQuestLine)button;
			
			if(selected != null)
			{
				qlDesc.SetText(I18n.format(selected.getQuestLine().getUnlocalisedDescription()));
				qlGui.setQuestLine(selected.getButtonTree(), true);
			}
		}
	}
	
	@Override
	public void mouseClicked(int mx, int my, int click) throws IOException
	{
		super.mouseClicked(mx, my, click);
		
		if(click != 0)
		{
			return;
		}
		
		QuestLineButtonTree tree = qlGui.getQuestLine();
		
		if(tree != null)
		{
			int rmx = qlGui.getRelativeX(mx);
			int rmy = qlGui.getRelativeY(my);
			
			GuiButtonQuestInstance btn = tree.getButtonAt(rmx, rmy);
			
			if(btn != null && btn.visible && (btn.enabled || QuestSettings.INSTANCE.canUserEdit(mc.player)))
			{
				btn.playPressSound(mc.getSoundHandler());
				bookmarked = new GuiQuestInstance(this, btn.getQuest());
				mc.displayGuiScreen(bookmarked);
				return;
			}
		}
		
		GuiButtonThemed btn = qlBtnList.getButtonUnderMouse(mx, my);
		
		if(btn != null && btn.mousePressed(mc, mx, my))
		{
			btn.playPressSound(mc.getSoundHandler());
			this.actionPerformed(btn);
		}
	}
}
