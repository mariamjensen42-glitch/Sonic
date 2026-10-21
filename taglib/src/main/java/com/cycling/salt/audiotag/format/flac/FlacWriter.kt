/*
 * Salt Audio Tag
 * Copyright (C) 2025 Moriafly
 *
 * This library is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA
 */

package com.cycling.salt.audiotag.format.flac

import com.cycling.salt.audiotag.UnstableAudioTagApi
import com.cycling.salt.audiotag.rw.WriteOperation
import com.cycling.salt.audiotag.rw.Writer
import com.cycling.salt.audiotag.rw.data.Picture
import com.cycling.salt.audiotag.util.SystemFileSystemUtil
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.write

class FlacWriter : Writer {
    private val metadataBlocks = mutableListOf<MetadataBlock>()

    @UnstableAudioTagApi
    private fun getWriteMetadataBlockDatas(
        operation: WriteOperation.AllMetadata?
    ): List<MetadataBlockData> {
        val writeMetadataBlockDatas = metadataBlocks.map { it.data }.toMutableList()

        if (operation == null) {
            return writeMetadataBlockDatas
        }

        val metadatas = operation.metadatas
        val pictures = operation.pictures

        // --- Handle VorbisComment ---

        val vorbisCommentIndex = writeMetadataBlockDatas.indexOfFirst {
            it is MetadataBlockDataVorbisComment
        }
        val vorbisComment = writeMetadataBlockDatas
            .find { it is MetadataBlockDataVorbisComment }

        if (vorbisComment != null) {
            if (metadatas.isEmpty()) {
                writeMetadataBlockDatas.remove(vorbisComment)
            } else {
                val newVorbisComment = MetadataBlockDataVorbisComment(
                    vendorString = (
                        vorbisComment as MetadataBlockDataVorbisComment
                    ).vendorString,
                    userComments = metadatas.map { it.toFlacUserComment() }
                )
                writeMetadataBlockDatas[vorbisCommentIndex] = newVorbisComment
            }
        } else {
            if (metadatas.isEmpty()) {
                // Do nothing
            } else {
                val newVorbisComment = MetadataBlockDataVorbisComment(
                    vendorString = VENDOR_STRING,
                    userComments = metadatas.map { it.toFlacUserComment() }
                )
                writeMetadataBlockDatas.add(newVorbisComment)
            }
        }

        // --- Handle Pictures ---

        // Remove all existing Picture blocks
        writeMetadataBlockDatas.removeAll { it is MetadataBlockDataPicture }

        if (pictures != null) {
            for (pic in pictures) {
                writeMetadataBlockDatas.add(pic.toMetadataBlockDataPicture())
            }
        }

        return writeMetadataBlockDatas
    }

    /** Convert public [Picture] to internal [MetadataBlockDataPicture]. */
    private fun Picture.toMetadataBlockDataPicture(): MetadataBlockDataPicture =
        MetadataBlockDataPicture(
            pictureType = pictureType.toFlacPictureTypeInt(),
            mediaType = mediaType,
            description = description,
            width = width,
            height = height,
            colorDepth = colorDepth,
            colorsNumber = colorsNumber,
            pictureData = pictureData,
        )

    /** Map [Picture.PictureType] enum back to FLAC picture type integer. */
    private fun Picture.PictureType.toFlacPictureTypeInt(): Int = when (this) {
        Picture.PictureType.Other -> 0
        Picture.PictureType.PngFileIcon32x32 -> 1
        Picture.PictureType.GeneralFileIcon -> 2
        Picture.PictureType.FrontCover -> 3
        Picture.PictureType.BackCover -> 4
        Picture.PictureType.LinerNotesPage -> 5
        Picture.PictureType.MediaLabel -> 6
        Picture.PictureType.Lead -> 7
        Picture.PictureType.Artist -> 8
        Picture.PictureType.Conductor -> 9
        Picture.PictureType.Band -> 10
        Picture.PictureType.Composer -> 11
        Picture.PictureType.Lyricist -> 12
        Picture.PictureType.RecordingLocation -> 13
        Picture.PictureType.DuringRecording -> 14
        Picture.PictureType.DuringPerformance -> 15
        Picture.PictureType.MovieScreenCapture -> 16
        Picture.PictureType.BrightColoredFish -> 17
        Picture.PictureType.Illustration -> 18
        Picture.PictureType.BandLogo -> 19
        Picture.PictureType.PublisherLogotype -> 20
        Picture.PictureType.Unknown -> 127
    }

    @UnstableAudioTagApi
    override fun write(src: Path, dst: Path, vararg operation: WriteOperation) {
        val tempFilePath = SystemFileSystemUtil.tempFilePath()
        try {
            SystemFileSystemUtil.copy(src, tempFilePath)

            SystemFileSystem.source(src).buffered().use { source ->
                // Read and create new metadata blocks
                FlacSignature(source)

                var metadataBlockHeader: MetadataBlockHeader
                do {
                    metadataBlockHeader = MetadataBlockHeader.create(source)

                    val metadataBlockData = when (metadataBlockHeader.blockType) {
                        BlockType.Streaminfo ->
                            MetadataBlockDataStreaminfo.create(source)
                        BlockType.Padding ->
                            MetadataBlockDataPadding.create(source, metadataBlockHeader.length)
                        BlockType.Application ->
                            MetadataBlockDataApplication.create(source, metadataBlockHeader.length)
                        BlockType.Seektable ->
                            MetadataBlockDataSeektable.create(source, metadataBlockHeader.length)
                        BlockType.VorbisComment ->
                            MetadataBlockDataVorbisComment.create(source)
                        BlockType.Cuesheet ->
                            MetadataBlockDataCuesheet.create(source, metadataBlockHeader.length)
                        BlockType.Picture ->
                            MetadataBlockDataPicture.create(source)
                        else ->
                            error(
                                "Unsupported metadata block type: ${metadataBlockHeader.blockType}"
                            )
                    }

                    metadataBlocks.add(
                        MetadataBlock(
                            header = metadataBlockHeader,
                            data = metadataBlockData
                        )
                    )
                } while (!metadataBlockHeader.isLastMetadataBlock)

                val operationAllMetadata = operation
                    .find { it is WriteOperation.AllMetadata }
                    as WriteOperation.AllMetadata?

                val writeMetadataBlockDatas = getWriteMetadataBlockDatas(operationAllMetadata)

                // Write to temp
                SystemFileSystem.sink(tempFilePath).buffered().use { sink ->
                    sink.write(FlacSignature.HEADER)
                    writeMetadataBlockDatas.forEachIndexed { index, data ->
                        val dataByteString = data.toByteString()

                        sink.write(
                            MetadataBlockHeader(
                                isLastMetadataBlock = index == writeMetadataBlockDatas.lastIndex,
                                blockType = data.blockType,
                                length = dataByteString.size
                            ).toByteString()
                        )
                        sink.write(dataByteString)
                    }

                    sink.transferFrom(source)
                }

                // Atomic move
                SystemFileSystem.atomicMove(tempFilePath, dst)
            }
        } finally {
            if (SystemFileSystem.exists(tempFilePath)) {
                SystemFileSystem.delete(tempFilePath)
            }
        }
    }

    companion object {
        // 上游这里拼的是 Library version（buildkonfig 生成）。vendored 进 Sonic 后去掉了
        // buildkonfig，Vorbis comment 的 vendor string 用固定值即可。
        private val VENDOR_STRING = "Salt Audio Tag"
    }
}
