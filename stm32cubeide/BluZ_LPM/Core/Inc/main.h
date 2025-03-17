/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.h
  * @brief          : Header for main.c file.
  *                   This file contains the common defines of the application.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2025 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */

/* Define to prevent recursive inclusion -------------------------------------*/
#ifndef __MAIN_H
#define __MAIN_H

#ifdef __cplusplus
extern "C" {
#endif

/* Includes ------------------------------------------------------------------*/
#include "stm32wbaxx_hal.h"
#include "app_conf.h"
#include "app_entry.h"
#include "app_common.h"
#include "app_debug.h"

#include "stm32wbaxx_ll_icache.h"
#include "stm32wbaxx_ll_tim.h"
#include "stm32wbaxx_ll_bus.h"
#include "stm32wbaxx_ll_cortex.h"
#include "stm32wbaxx_ll_rcc.h"
#include "stm32wbaxx_ll_system.h"
#include "stm32wbaxx_ll_utils.h"
#include "stm32wbaxx_ll_pwr.h"
#include "stm32wbaxx_ll_gpio.h"
#include "stm32wbaxx_ll_dma.h"

#include "stm32wbaxx_ll_exti.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include <stdbool.h>
#include "rw_FLASH.h"
#include <stm32_timer.h>
#include <stm32_seq.h>
#include <stm32_lpm.h>
#include "LTC1662.h"
/* USER CODE END Includes */

/* Exported types ------------------------------------------------------------*/
/* USER CODE BEGIN ET */
/*
 * Размер буфера для служебных данных
 * Значение для вычисления максимального размера буфера
 * для разрешения 4096
 * 8192 + 104 =  34 * 244(MTU)
 */
#define DATA_NOTIFICATION_MAX_PACKET_SIZE (244U)
#define CRC_SIZE 1											/* размер в uint16_t */
#define MTU_SIZE DATA_NOTIFICATION_MAX_PACKET_SIZE			/* Размер mtu в uint8_t */
#define HEADER_OFFSET 40									/* Начало данных дозиметра (uint16_t)*/
#define SIZE_DOZIMETR_BUFER 512								/* Размер буфера для статистики дозиметра (uint16_t) */
#define LOG_BUFER_SIZE 50									/* Количество записей в логе */
#define LOG_OFFSET HEADER_OFFSET + SIZE_DOZIMETR_BUFER		/* Начало данных лога */
#define SPECTER_OFFSET LOG_OFFSET + LOG_BUFER_SIZE * 3		/* Начало спектра в буфере в uint16_t */
#define NUMBER_MTU_DOZR 6									/* Количество MTU для передачи накоплений дозиметра */
#define NUMBER_MTU_1024 16									/* Количество MTU для передачи спектра разрешением 1024 */
#define NUMBER_MTU_2048 23									/* Количество MTU для передачи спектра разрешением 2048 */
#define NUMBER_MTU_4096 40									/* Количество MTU для передачи спектра разрешением 4096 */
#define CHANNELS_1024	1024
#define CHANNELS_2048	2048
#define CHANNELS_4096	4096
#define MAX_RESOLUTION 4096

extern uint16_t MTUSizeValue;
extern uint8_t resolution, dataType;
extern uint16_t transmitBuffer[NUMBER_MTU_4096 * 244 / 2 + SPECTER_OFFSET];
extern uint16_t currTemperature, currVoltage,tmpSpecterBuffer[MAX_RESOLUTION];
extern uint32_t currentTimeAvg, pulseCounterAvg, pulseCounter, currentTime, pulseCounterSecond, CPS, intervalNow, TVLevel[3];
extern bool SoundEnable, VibroEnable, LEDEnable;
extern bool levelSound1, levelSound2, levelSound3;
extern bool levelVibro1, levelVibro2, levelVibro3;
extern bool flagTemperatureMess, autoStartSpecrometr;
extern uint16_t HVoltage, comparatorLevel;
extern LPTIM_HandleTypeDef hlptim2;
extern uint16_t dozimetrBuffer[SIZE_DOZIMETR_BUFER];
extern int indexDozimetrBufer;
extern ADC_HandleTypeDef hadc4;
extern uint64_t PL[8];

struct LG {
	uint32_t time;
	uint8_t type;
};

union dataC {
	float Float;
	uint16_t Uint16[2];
	uint8_t Uint[4];
};

union dataA {
	float Float;
	uint16_t Uint[2];
};

extern uint32_t level1_cps, level2_cps, level3_cps, tmp_level;
extern uint16_t level1, level2, level3;
extern union dataC calcCoeff;
extern union dataC enCoefA, enCoefB, enCoefC;
extern union dataA Temperature, Voltage;
extern union dataA AvgCPS;
extern uint16_t MTUSizeValue;

void sendData( uint8_t *dataSpectrBufer );
/* USER CODE END ET */

/* Exported constants --------------------------------------------------------*/
/* USER CODE BEGIN EC */

/* USER CODE END EC */

/* Exported macro ------------------------------------------------------------*/
/* USER CODE BEGIN EM */

/* USER CODE END EM */

void HAL_LPTIM_MspPostInit(LPTIM_HandleTypeDef *hlptim);

/* Exported functions prototypes ---------------------------------------------*/
void Error_Handler(void);
void MX_GPIO_Init(void);
void MX_GPDMA1_Init(void);
void MX_ADC4_Init(void);
void MX_CRC_Init(void);
void MX_ICACHE_Init(void);
void MX_RAMCFG_Init(void);
void MX_RNG_Init(void);
void MX_RTC_Init(void);
void MX_LPTIM1_Init(void);
void MX_LPTIM2_Init(void);
void MX_TIM17_Init(void);

/* USER CODE BEGIN EFP */
void NotifyAct(uint8_t SRC, uint32_t repCnt);
/* USER CODE END EFP */

/* Private defines -----------------------------------------------------------*/
#define A_DATA_Pin GPIO_PIN_12
#define A_DATA_GPIO_Port GPIOB
#define A_SCK_Pin GPIO_PIN_8
#define A_SCK_GPIO_Port GPIOA
#define A_CS_Pin GPIO_PIN_7
#define A_CS_GPIO_Port GPIOA
#define VIBRO_Pin GPIO_PIN_6
#define VIBRO_GPIO_Port GPIOA
#define LED_Pin GPIO_PIN_5
#define LED_GPIO_Port GPIOA
#define batLev_Pin GPIO_PIN_2
#define batLev_GPIO_Port GPIOA
#define SOUND_Pin GPIO_PIN_1
#define SOUND_GPIO_Port GPIOA
#define NC_Pin GPIO_PIN_9
#define NC_GPIO_Port GPIOB
#define NCB8_Pin GPIO_PIN_8
#define NCB8_GPIO_Port GPIOB
#define Sync_Pin GPIO_PIN_15
#define Sync_GPIO_Port GPIOB
#define Sync_EXTI_IRQn EXTI15_IRQn

/* USER CODE BEGIN Private defines */
#define INTERVAL1 1
#define INTERVAL2 5
#define INTERVAL3 1
#define INTERVAL4 10

#define SOUND_TIME_NOTIFY 4096
#define SOUND_NOTIFY 	1
#define VIBRO_NOTIFY 	2
#define LED_NOTIFY   	4
#define TEST_LED		1
#define MEASURE_INTERVAL	1000

//#define DEBUG_USER

/*
 * TODO -- Нужно получить реальное напряжение питания
 */
#define ADC_VREF 2.8f					// Напряжение питания.
#define ADC_VREF_COEF 4.2f / 4080.0f

/* USER CODE END Private defines */

#ifdef __cplusplus
}
#endif

#endif /* __MAIN_H */
