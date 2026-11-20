/*
 * spectrum_codec.h
 *
 * Сжатие спектра для протокола <B> версии 1: DPCM + ZigZag + varint base-128
 * (младшая группа первой). См. Android/BluZ_v2/BLE_PROTOCOL.md
 */
#ifndef INC_SPECTRUM_CODEC_H_
#define INC_SPECTRUM_CODEC_H_

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>

/* Состояние потокового кодировщика: отсчеты подаются по одному, без промежуточного массива */
typedef struct {
	uint8_t *out;		/* Начало выходного буфера */
	uint8_t *p;			/* Текущая позиция записи */
	uint8_t *limit;		/* Конец выходного буфера */
	uint32_t prev;		/* Предыдущий отсчет */
} spectrum_encoder_t;

void spectrum_encoder_init(spectrum_encoder_t *enc, uint8_t *out, size_t out_capacity);
bool spectrum_encoder_put(spectrum_encoder_t *enc, uint32_t cur);
size_t spectrum_encoder_size(const spectrum_encoder_t *enc);

#endif /* INC_SPECTRUM_CODEC_H_ */
