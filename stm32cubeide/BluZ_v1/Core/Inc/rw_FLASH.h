/*
 * rw_FLASH.h
 *
 *  Created on: Jul 8, 2024
 *      Author: ed
 */

#ifndef INC_RW_FLASH_H_
#define INC_RW_FLASH_H_

//#include <simple_nvm_arbiter.h>
//#include <rf_timing_synchro.h>
//#include <flash_manager.h>
#include <flash_driver.h>
#include "main.h"

#define FLASH_BLOCK_SIZE 0x2000												// Размер блока 8k
#define START_FLASH_ADDRESS FLASH_BASE_NS + FLASH_SIZE - FLASH_BLOCK_SIZE	// Адрес последнего блока
#define MAGIC_KEY 0x01234567												// Ключ для определения была ли инициализация параметров.
#define MAGIC_KEY_ADDRESS START_FLASH_ADDRESS								// Адрес для хранения ключа
#define PARAMETERS_ADDRESS START_FLASH_ADDRESS + 2							// Адрес для хранения параметров управления светодиодом, вибро и звуком
#define KOEF_A_ADDRESS START_FLASH_ADDRESS + 4								// Адрес коэффициента A преобразования канала в энергию.
#define KOEF_B_ADDRESS START_FLASH_ADDRESS + 8								// Адрес коэффициента B преобразования канала в энергию.
#define KOEF_C_ADDRESS START_FLASH_ADDRESS + 12								// Адрес коэффициента C преобразования канала в энергию.
#define LEVEL1_ADDRESS START_FLASH_ADDRESS + 16								// Адрес заначения первого порога
#define LEVEL2_ADDRESS START_FLASH_ADDRESS + 20								// Адрес значения второго порога
#define LEVEL3_ADDRESS START_FLASH_ADDRESS + 24								// Адрес значения третьего прога
#define CPS2RH_ADDRESS START_FLASH_ADDRESS + 28								// Адрес коэффициента пересчета cps в uRh


extern char uartBuffer[100];
extern UART_HandleTypeDef huart2;

HAL_StatusTypeDef writeFlash();
HAL_StatusTypeDef readFlash();

#endif /* INC_RW_FLASH_H_ */
