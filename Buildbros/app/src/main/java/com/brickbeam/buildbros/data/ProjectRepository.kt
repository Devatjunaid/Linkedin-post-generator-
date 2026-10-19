package com.brickbeam.buildbros.data

import com.brickbeam.buildbros.calc.Category
import com.brickbeam.buildbros.data.db.LineItemEntity
import com.brickbeam.buildbros.data.db.ProjectDao
import com.brickbeam.buildbros.data.db.ProjectEntity

/** A material line produced by a calculator, before it is saved into a project. */
data class DraftItem(
    val category: Category,
    val name: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val note: String,
)

class ProjectRepository(private val dao: ProjectDao) {
    val summaries = dao.observeSummaries()
    val projects = dao.observeProjects()

    fun project(id: Long) = dao.observeProject(id)
    fun items(projectId: Long) = dao.observeItems(projectId)

    suspend fun getProject(id: Long) = dao.getProject(id)
    suspend fun getItems(projectId: Long) = dao.getItems(projectId)

    suspend fun createProject(name: String, regionCode: String): Long =
        dao.insertProject(ProjectEntity(name = name.trim().ifEmpty { "My project" }, regionCode = regionCode))

    suspend fun updateProject(project: ProjectEntity) =
        dao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteProject(id: Long) = dao.deleteProject(id)

    suspend fun addItems(projectId: Long, drafts: List<DraftItem>) {
        val now = System.currentTimeMillis()
        dao.insertItems(
            drafts.filter { it.quantity > 0 }.mapIndexed { i, d ->
                LineItemEntity(
                    projectId = projectId, category = d.category.name, name = d.name,
                    quantity = d.quantity, unit = d.unit, unitPrice = d.unitPrice,
                    note = d.note, createdAt = now + i,
                )
            },
        )
        dao.touch(projectId)
    }

    suspend fun updateItem(item: LineItemEntity) {
        dao.updateItem(item)
        dao.touch(item.projectId)
    }

    suspend fun deleteItem(item: LineItemEntity) {
        dao.deleteItem(item.id)
        dao.touch(item.projectId)
    }
}
