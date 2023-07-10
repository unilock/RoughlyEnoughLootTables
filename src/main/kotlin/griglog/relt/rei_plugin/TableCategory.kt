package griglog.relt.rei_plugin

import griglog.relt.RELT
import griglog.relt.table_storage.openTableJson
import me.shedaniel.clothconfig2.ClothConfigInitializer
import me.shedaniel.clothconfig2.api.scroll.ScrollingContainer
import me.shedaniel.math.Point
import me.shedaniel.math.Rectangle
import me.shedaniel.rei.api.client.REIRuntime
import me.shedaniel.rei.api.client.gui.widgets.Slot
import me.shedaniel.rei.api.client.gui.widgets.Widget
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds
import me.shedaniel.rei.api.client.gui.widgets.Widgets
import me.shedaniel.rei.api.client.registry.display.DisplayCategory
import me.shedaniel.rei.api.common.category.CategoryIdentifier
import me.shedaniel.rei.api.common.entry.EntryIngredient
import me.shedaniel.rei.api.common.entry.EntryStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth

val categoryId: CategoryIdentifier<TableDisplay> = CategoryIdentifier.of(RELT.id, "plugin")

class TableCategory : DisplayCategory<TableDisplay> {
    override fun getCategoryIdentifier() = categoryId

    override fun getTitle() = Component.translatable(RELT.id + ".category")

    override fun getIcon() = EntryStack.of(TableEntryDef.type, TableEntryDef.rootId)

    override fun getDisplayHeight() = 150

    override fun getFixedDisplaysPerPage() = 1

    override fun setupDisplay(display: TableDisplay, bounds: Rectangle): MutableList<Widget> {
        val center = Point(bounds.centerX, bounds.centerY)
        val widgets = mutableListOf<Widget>()

        val hasInputs = display.inputEntries.size == 2
        if (hasInputs) {
            widgets.add(Widgets.createSlot(Point(center.x - 20, bounds.y + 10))
                .entries(display.inputEntries[1]).markInput())
        }
        widgets.add(Widgets.createSlot(Point(center.x + if (hasInputs) 0 else -10, bounds.y + 10))
            .entries(display.inputEntries[0]))

        val outBounds = Rectangle(bounds.x, bounds.y + 30, bounds.width, bounds.height - 30)
        widgets.add(Widgets.createSlotBase(outBounds))
        widgets.add(ScrollableSlotsWidget(outBounds, display.outputEntries))

        widgets.add(Widgets.createButton(Rectangle(bounds.maxX + 2, bounds.maxY - 30, 10, 10), Component.literal("J"))
            .tooltipLine(Component.translatable(RELT.id + ".json_button"))
            .onClick { button -> openTableJson(display.displayLocation.get())})

        return widgets
    }
}

//copy-paste from Beacon Payment
class ScrollableSlotsWidget : WidgetWithBounds {
    private val bounds: Rectangle
    private val widgets: List<Slot>
    private val scrolling: ScrollingContainer

    constructor(bounds: Rectangle, ings: Collection<EntryIngredient>){
        this.bounds = bounds
        widgets = ings.map{
            Widgets.createSlot(Point(0, 0))
                .entries(it)
                .disableBackground()
        }
        scrolling = object : ScrollingContainer() {
            override fun getBounds(): Rectangle {
                val r = this@ScrollableSlotsWidget.getBounds()
                return Rectangle(r)
            }

            override fun getMaxScrollHeight(): Int {
                return Mth.ceil(widgets.size / 8f) * 18
            }
        }
    }

    override fun getBounds(): Rectangle = bounds

    override fun children(): List<GuiEventListener?> = widgets

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        if (containsMouse(mouseX, mouseY)) {
            scrolling.offset(ClothConfigInitializer.getScrollStep() * -delta, true)
            return true
        }
        return false
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean =
        scrolling.updateDraggingState(mouseX, mouseY, button) ||
                super.mouseClicked(mouseX, mouseY, button)

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean =
        scrolling.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
                || super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)


    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        scrolling.updatePosition(delta)
        val innerBounds = scrolling.scissorBounds
        scissor(graphics, innerBounds).use { scissors ->
            for (y in 0 until Mth.ceil(widgets.size / 8f)) {
                for (x in 0..7) {
                    val index = y * 8 + x
                    if (widgets.size <= index)
                        return@use
                    val widget = widgets[index]
                    widget.bounds.setLocation(bounds.x + 1 + x * 18, bounds.y + 1 + y * 18 - scrolling.scrollAmountInt())
                    widget.render(graphics, mouseX, mouseY, delta)
                }
            }
        }
        scissor(graphics, scrolling.bounds).use { scissors ->
            scrolling.renderScrollBar(
                graphics,
                -0x1000000,
                1f,
                if (REIRuntime.getInstance().isDarkThemeEnabled) 0.8f else 1f
            )
        }
    }
}
