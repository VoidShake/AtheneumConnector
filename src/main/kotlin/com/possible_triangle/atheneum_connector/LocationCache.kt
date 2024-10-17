package com.possible_triangle.atheneum_connector

import com.mojang.datafixers.util.Either
import com.possible_triangle.atheneum_connector.generated.AreasQuery
import com.possible_triangle.atheneum_connector.generated.PlacesQuery
import com.possible_triangle.atheneum_connector.generated.areasquery.Area
import com.possible_triangle.atheneum_connector.generated.areasquery.FlatPoint
import com.possible_triangle.atheneum_connector.generated.placesquery.Place
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.minecraft.core.BlockPos
import net.minecraft.core.SectionPos.blockToSectionCoord
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level

class LocationCache<T>(private val getId: (T) -> Int) {

    private val lookup = hashMapOf<ResourceKey<Level>, MutableMap<ChunkPos, T>>()
    private val reverseLookup = hashMapOf<Int, Pair<ResourceKey<Level>, List<ChunkPos>>>()

    fun get(level: ResourceKey<Level>, chunk: ChunkPos): T? {
        return lookup[level]?.get(chunk)
    }

    fun set(level: ResourceKey<Level>, value: T, chunks: List<ChunkPos>) {
        reverseLookup[getId(value)] = level to chunks

        lookup.getOrPut(level, ::hashMapOf).apply {
            chunks.forEach { put(it, value) }
        }
    }

    companion object {
        private val AREAS = LocationCache<Area> { it.id }
        private val PLACES = LocationCache<Place> { it.id }

        fun reload() {
            AREAS.clear()
            PLACES.clear()
            initialize()
        }

        private fun String.levelKey() = ResourceKey.create(Registries.DIMENSION, ResourceLocation(this))

        private fun chunkAt(x: Int, z: Int) = ChunkPos(
            blockToSectionCoord(x),
            blockToSectionCoord(z),
        )

        fun initialize() = runBlocking {
            val areasQuery = async { GraphQL.query(AreasQuery()) }
            val placesQuery = async { GraphQL.query(PlacesQuery()) }

            areasQuery.await().areas.nodes.forEach { area ->
                val level = area.world.levelKey()
                val chunks = area.points.containingChunks()
                AREAS.set(level, area, chunks)
            }

            placesQuery.await().places.nodes.forEach { place ->
                val level = place.pos.world.levelKey()
                val chunks = listOf(chunkAt(place.pos.x, place.pos.z))

                PLACES.set(level, place, chunks)
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
            }.map { chunkAt(it.x, it.z) }
        }

        fun containing(level: ResourceKey<Level>, pos: BlockPos): Either<Area, Place>? {
            val chunk = ChunkPos(pos)

            return PLACES.get(level, chunk)?.let {
                Either.right(it)
            } ?: AREAS.get(level, chunk)?.let {
                Either.left(it)
            }
        }
    }

    private fun clear() {
        lookup.clear()
        reverseLookup.clear()
    }

}