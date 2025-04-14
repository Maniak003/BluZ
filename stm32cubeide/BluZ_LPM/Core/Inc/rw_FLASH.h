/*
 * rw_FLASH.h
 *
 *  Created on: Jul 8, 2024
 *      Author: ed
 */

#ifndef INC_RW_FLASH_H_
#define INC_RW_FLASH_H_

#include <simple_nvm_arbiter.h>
#include <rf_timing_synchro.h>
//#include <flash_manager.h>
#include <flash_driver.h>
#include "main.h"

#define FLASH_BLOCK_SIZE 0x04000											// Размер блока 8k
#define FLASH_CONFIG_OFFSET 6 * 4											// Смещение буфера конфигурации в NVM
#define START_FLASH_ADDRESS FLASH_BASE_NS + FLASH_SIZE - FLASH_BLOCK_SIZE	// Адрес последнего блока
#define MAGIC_KEY 0x01234567000000AA										// Ключ для определения была ли инициализация параметров.
#define MAGIC_KEY_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET			// Адрес для хранения ключа
#define PARAMETERS_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 8	// Адрес для хранения параметров управления светодиодом, вибро и звуком
#define CONVERT_CPS2RH START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 12		// Коэффициент преобразования CPS в uRh
#define LEVEL1_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 16		// Адрес заначения первого порога
#define LEVEL2_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 20		// Адрес значения второго порога
#define LEVEL3_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 24		// Адрес значения третьего порога
#define KOEF_A1024_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 32	// Адрес коэффициента A преобразования канала в энергию для 1024 каналов.
#define KOEF_B1024_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 36	// Адрес коэффициента B преобразования канала в энергию для 1024 каналов.
#define KOEF_C1024_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 40	// Адрес коэффициента C преобразования канала в энергию для 1024 каналов.
#define KOEF_A2048_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 44	// Адрес коэффициента A преобразования канала в энергию для 2048 каналов.
#define KOEF_B2048_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 48	// Адрес коэффициента B преобразования канала в энергию для 2048 каналов.
#define KOEF_C2048_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 52	// Адрес коэффициента C преобразования канала в энергию для 2048 каналов.
#define KOEF_A4096_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 54	// Адрес коэффициента A преобразования канала в энергию для 4096 каналов.
#define KOEF_B4096_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 58	// Адрес коэффициента B преобразования канала в энергию для 4096 каналов.
#define KOEF_C4096_ADDRESS START_FLASH_ADDRESS + FLASH_CONFIG_OFFSET + 62	// Адрес коэффициента C преобразования канала в энергию для 4096 каналов.

//extern uint64_t buffer_nvm[CFG_BLEPLAT_NVM_MAX_SIZE];
//extern char uartBuffer[400];
//extern UART_HandleTypeDef huart2;
//extern uint64_t buffer_nvm[CFG_BLEPLAT_NVM_MAX_SIZE];

//HAL_StatusTypeDef writeFlash();
//HAL_StatusTypeDef readFlash();
//void NVM_Init( uint64_t* buffer,
//               uint16_t size,
//               uint16_t max_size );
HAL_StatusTypeDef writeFlash(void);
HAL_StatusTypeDef readFlash(void);

#endif /* INC_RW_FLASH_H_ */
