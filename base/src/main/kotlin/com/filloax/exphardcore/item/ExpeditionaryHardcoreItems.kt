package com.filloax.exphardcore.item

import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.registration.registryDelegate
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.*
import net.minecraft.world.item.Item.Properties
import net.minecraft.world.item.component.WritableBookContent

object ExpeditionaryHardcoreItems {
	val all = mutableMapOf<Identifier, Item>()
	private val allInitializers = mutableMapOf<Identifier, () -> Item>()

	val EXPEDITIONERS_LOGBOOK by make("expeditioner_logbook") { props ->
		ExpeditionersLogbookItem(
			props
				.component(DataComponents.WRITABLE_BOOK_CONTENT, WritableBookContent.EMPTY)
				.rarity(Rarity.UNCOMMON)
				.stacksTo(1)
				.fireResistant()
		)
	}

	// supplier gets a Properties with the registry id already set, since Item's
	// constructor requires it (throws "Item id not set" otherwise)
	private inline fun <reified T : Item> make(name: String, noinline supplier: (Properties) -> T) = registryDelegate(
		id(name)
	) {
		if (allInitializers.containsKey(id)) {
			throw IllegalArgumentException("Item $id already registered!")
		}

		allInitializers[id] = {
			val item = supplier(defaultBuilder(name))
			all[id] = item
			init(item)
			item
		}
	}

	fun registerItems(registrator: (Identifier, Item) -> Unit) {
		allInitializers.forEach{
			registrator(it.key, it.value())
		}
	}

	private fun defaultBuilder(name: String) = Properties().setId(ResourceKey.create(Registries.ITEM, id(name)))
}