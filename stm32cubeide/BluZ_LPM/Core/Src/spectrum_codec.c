/*
 *	Сжатие спектра: DPCM + ZigZag + varint base-128.
 *
 *	Отсчеты uint32_t, разность считается по модулю 2^32, prev = 0 в начале каждого кадра.
 *	Пример: [0, 1, 0, 65] -> 00 02 01 82 01
 */

#include "spectrum_codec.h"

/*
 * Начинает кодирование в out. Размер каждого следующего varint проверяется по остатку буфера,
 * поэтому поток, который помещается в out_capacity, не отбрасывается из-за запаса в 5 байт.
 */
void spectrum_encoder_init(spectrum_encoder_t *enc, uint8_t *out, size_t out_capacity)
{
	enc->out = out;
	enc->p = out;
	enc->limit = out + out_capacity;
	enc->prev = 0;
}

/*
 * Кодирует следующий отсчет.
 *
 * Возвращает false, если он не помещается в буфер: кодирование нужно прервать.
 */
bool spectrum_encoder_put(spectrum_encoder_t *enc, uint32_t cur)
{
	uint32_t delta = cur - enc->prev;
	uint32_t zigzag = (delta << 1) ^ (0u - (delta >> 31));

	/* Размер varint: 1 байт на каждые 7 бит значения */
	size_t len = 1;
	for (uint32_t tmp = zigzag >> 7; tmp != 0; tmp >>= 7) {
		len++;
	}
	if ((size_t)(enc->limit - enc->p) < len) {
		return false;
	}
	enc->prev = cur;

	while (zigzag >= 0x80u) {
		*enc->p++ = (uint8_t)(zigzag | 0x80u);
		zigzag >>= 7;
	}
	*enc->p++ = (uint8_t)zigzag;
	return true;
}

/* Число байт, записанных с начала кодирования */
size_t spectrum_encoder_size(const spectrum_encoder_t *enc)
{
	return (size_t)(enc->p - enc->out);
}
