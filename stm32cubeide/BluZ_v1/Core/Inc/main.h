/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.h
  * @brief          : Header for main.c file.
  *                   This file contains the common defines of the application.
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2024 STMicroelectronics.
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

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */

/* USER CODE END Includes */

/* Exported types ------------------------------------------------------------*/
/* USER CODE BEGIN ET */
/*
 * Размер буфера для служебных данных
 * Значение для вычисления максимального размера буфера
 * для разрешения 4096
 * 8192 + 104 =  34 * 244(MTU)
 */
#define CRC_SIZE 1							/* размер в uint16_t */
#define MTU_SIZE 244						/* Размер mtu в uint8_t */
#define HEADER_OFFSET_2048 74 - CRC_SIZE	/* размер в uint16_t */
#define HEADER_OFFSET_4096 26 - CRC_SIZE	/* размер в uint16_t */
#define HEADER_OFFSET_8192 52 - CRC_SIZE	/* размер в uint16_t */
#define NUMBER_MTU_1024 9					/* Количество MTU для передачи спектра разрешением 1024 */
#define NUMBER_MTU_2048 17					/* Количество MTU для передачи спектра разрешением 2048 */
#define NUMBER_MTU_4096 34					/* Количество MTU для передачи спектра разрешением 4096 */
#define NUMBER_MTU_DOZR 4					/* Количество MTU для передачи накоплений дозиметра */
#define NUMBER_MTU_LOG 3					/* Количество MTU для передачи логов */
#define NUMBER_MTU_PARAM 1					/* Количество MTU для передачи текущих параметров прибора */
#define SIZE_BUF_1024 1098					/* Итоговый размер буфера в uint16_t*/
#define SIZE_BUF_2048 2074
#define SIZE_BUF_4096 4148
#define SIZE_BUF_DOZR 488
#define SIZE_BUF_LOG 366
#define SIZE_BUF_PARAM 122
#define MAX_RESOLUTION 4096

#define DATA_NOTIFICATION_MAX_PACKET_SIZE (244U)
extern uint16_t MTUSizeValue;
extern uint8_t resolution;
extern uint16_t specterBuffer[SIZE_BUF_4096];
extern uint16_t tmpSpecterBuffer[MAX_RESOLUTION];
extern uint32_t pulseCounter, pulseLevel;
extern bool SoundEnable, VibroEnable, LEDEnable;
void sendData(uint8_t *dataSpectrBufer);

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
void MX_RTC_Init(void);

/* USER CODE BEGIN EFP */
void NotifyAct(uint8_t SRC);
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
#define AIn_Pin GPIO_PIN_0
#define AIn_GPIO_Port GPIOA
#define TX_Pin GPIO_PIN_12
#define TX_GPIO_Port GPIOA
#define Sync_Pin GPIO_PIN_15
#define Sync_GPIO_Port GPIOB
#define Sync_EXTI_IRQn EXTI15_IRQn

/* USER CODE BEGIN Private defines */
#define INTERVAL1 2000
#define INTERVAL2 5000
#define INTERVAL3 2000
#define SOUND_NOTIFY 1
#define VIBRO_NOTIFY 2
#define LED_NOTIFY   4
/* USER CODE END Private defines */

#ifdef __cplusplus
}
#endif

#endif /* __MAIN_H */
