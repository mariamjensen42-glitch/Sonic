# Checklist

- [x] 所有模型类迁移完成到 data/lyrics/model，包名修改为 com.cycling.data.lyrics.model
- [x] 所有工具类迁移完成到 data/lyrics/utils，移除 KugouKrcMetadataDecoder
- [x] 所有解析器迁移完成到 data/lyrics/parser，AutoParser 中移除 KUGOU_KRC 注册
- [x] LyricsFormatGuesser 中移除 KUGOU_KRC 格式检测
- [x] 所有导出器迁移完成到 data/lyrics/exporter
- [x] 代码中无酷狗相关引用
