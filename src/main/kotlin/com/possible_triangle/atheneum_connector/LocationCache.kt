package com.possible_triangle.atheneum_connector

import com.mojang.datafixers.util.Either
import com.possible_triangle.atheneum_connector.generated.AreasQuery
import com.possible_triangle.atheneum_connector.generated.areasquery.Area
import com.possible_triangle.atheneum_connector.generated.areasquery.FlatPoint
import com.possible_triangle.atheneum_connector.generated.placesquery.Place
import kotlinx.coroutines.runBlocking
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos.blockToSectionCoord
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level

class LocationCache<T> {

    private val lookup = hashMapOf<ResourceKey<Level>, MutableMap<ChunkPos, T>>()
    private val reverseLookup = hashMapOf<Int, Pair<ResourceKey<Level>, List<ChunkPos>>>()

    companion object {
        private val AREAS = LocationCache<Area>()

        fun reload() {
            AREAS.clear()
            initialize()
        }

        fun initialize() = runBlocking {
            val areas = GraphQL.query(AreasQuery()).areas
            areas.nodes.forEach { area ->
                val level = ResourceKey.create(Registries.DIMENSION, ResourceLocation(area.world))
                val chunks = area.points.containingChunks()
                AREAS.reverseLookup[area.id] = level to chunks

                AREAS.lookup.getOrPut(level, ::hashMapOf).apply {
                    chunks.forEach { put(it, area) }
                }
            }
        }

        private fun List<FlatPoint>.containingChunks(): List<ChunkPos> {
            val xs = map { it.x }
            val zs = map { it.z }
            val minX = xs.min()
            val maxX = xs.max()
            val minZ = zs.min()
            val maxZ = zs.max()

            val possibleChunks = (minX..maxX step 16).flatMap { x ->
                (minZ..maxZ step 16).map { z ->
                    FlatPoint(x + 8, z + 8)
                }
            }

            return possibleChunks.filter { point ->
                var previous = last()
                var odd = false

                forEach { current ->
                    if (
                        (current.z > point.z) != (previous.z > point.z)
                        && (point.x < (previous.x - current.x) * (point.z - current.z) / (previous.z - current.z) + current.x)
                    ) {
                        odd = !odd
                    }
                    previous = current
                }

                odd
            }.map {
                ChunkPos(
                    blockToSectionCoord(it.x),
                    blockToSectionCoord(it.z),
                )
            }
        }

        fun containing(level: ResourceKey<Level>, pos: BlockPos): Either<Area, Place>? {
            val chunk = ChunkPos(pos)

            val area = AREAS.lookup[level]?.get(chunk)

            return area?.let { Either.left(area) }
        }
    }

    private fun clear() {
        lookup.clear()
        reverseLookup.clear()
    }

}