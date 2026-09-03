package com.aatmn2000.aibuilder.core.agent

/**
 * Names and wording for one application domain, shared by the agents,
 * the mock provider and the orchestrator.
 */
data class DomainProfile(
    val key: String,
    val entity: String,
    val service: String,
    val entityLower: String,
    val description: String,
    val namePrefix: String
)

/**
 * Deterministic mapping from a free-form request to a [DomainProfile].
 *
 * The mock provider relies on this so the whole pipeline is reproducible
 * without a model; real models produce the same layout because the agents
 * pass the profile to them as prompt tokens.
 */
object DomainProfiler {

    private val profiles = listOf(
        DomainProfile(
            key = "clinic_appointments",
            entity = "Appointment",
            service = "AppointmentService",
            entityLower = "appointment",
            description = "clinic appointment management",
            namePrefix = "Clinic"
        ),
        DomainProfile(
            key = "task_manager",
            entity = "Task",
            service = "TaskService",
            entityLower = "task",
            description = "task and to-do management",
            namePrefix = "Task"
        ),
        DomainProfile(
            key = "inventory_items",
            entity = "InventoryItem",
            service = "InventoryService",
            entityLower = "inventory item",
            description = "inventory and stock management",
            namePrefix = "Inventory"
        ),
        DomainProfile(
            key = "market_products",
            entity = "Product",
            service = "ProductService",
            entityLower = "product",
            description = "small shop and market management",
            namePrefix = "Market"
        ),
        DomainProfile(
            key = "library_books",
            entity = "Book",
            service = "LibraryService",
            entityLower = "book",
            description = "library and lending management",
            namePrefix = "Library"
        ),
        DomainProfile(
            key = "simple_tool",
            entity = "Item",
            service = "ItemService",
            entityLower = "item",
            description = "general purpose records",
            namePrefix = "My"
        )
    )

    private val keywordMap = mapOf(
        "clinic_appointments" to listOf("clinic", "appointment", "doctor", "patient", "medical", "health", "dental"),
        "task_manager" to listOf("task", "todo", "to-do", "project management", "workflow"),
        "inventory_items" to listOf("inventory", "stock", "warehouse", "spare parts"),
        "market_products" to listOf("shop", "store", "market", "sell", "sales", "retail", "order"),
        "library_books" to listOf("library", "book", "lend", "borrow")
    )

    fun allProfiles(): List<DomainProfile> = profiles

    fun defaultProfile(): DomainProfile = profiles.last()

    fun profileByKey(key: String): DomainProfile? = profiles.firstOrNull { it.key == key }

    fun profileFor(request: String): DomainProfile {
        val lowered = request.lowercase()
        keywordMap.forEach { (key, keywords) ->
            if (keywords.any { keyword -> lowered.contains(keyword) }) {
                return profiles.first { it.key == key }
            }
        }
        return defaultProfile()
    }
}

/** Derives a short display name for a generated project. */
object ProjectNamer {

    fun nameFor(profile: DomainProfile): String = "${profile.namePrefix}App"
}
