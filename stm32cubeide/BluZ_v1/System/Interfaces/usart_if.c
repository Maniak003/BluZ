/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file    usart_if.c
  * @author  MCD Application Team
  * @brief : Source file for interfacing the stm32_adv_trace to hardware
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

/*
 *	(!) No platform settings set in Cube MX for the MW STM32_WPAN. The Logs will not be used (!)
 *
 *	--------------------------------------------------------------------
 *	If you intend to use the Logs, please follow the procedure below :
 *	--------------------------------------------------------------------
 *
 *	1.	Open your project on Cube MX.
 *
 *	2.	Click on the MW "STM32_WPAN".
 *
 *	3.	Click on the "Configuration" panel.
 *
 *	4.	Open the sub-section "Application configuration - Logs".
 *
 *	5.	Enable one of the following according to your needs :
 *		. CFG_LOG_INSERT_TIME_STAMP_INSIDE_THE_TRACE
 *		. CFG_LOG_SUPPORTED
 *
 *	6.	Click on the "Platform Settings" panel.
 *
 *	7.	Select the BSP you'll use for the Logs. It can be one of the following :
 *		In order to select them, you need to activate the corresponding IP beforehand in Cube MX.
 *		. USART1
 *		. USART2
 *		. LPUART1
 *
 */

/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "stm32_adv_trace.h"
#include "usart_if.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */

/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
#define IRQ_BADIRQ       ((IRQn_Type)(-666))
/* USER CODE BEGIN PD */

/* USER CODE END PD */

/* External variables --------------------------------------------------------*/

/* USER CODE BEGIN EV */

/* USER CODE END EV */

/* Exported types ------------------------------------------------------------*/
/* USER CODE BEGIN ET */

/* USER CODE END ET */

/* Exported constants --------------------------------------------------------*/

/**
 *  @brief  trace tracer definition.
 *
 *  list all the driver interface used by the trace application.
 */
const UTIL_ADV_TRACE_Driver_s UTIL_TraceDriver =
{
  UART_Init,
  UART_DeInit,
  UART_StartRx,
  UART_TransmitDMA
};

/* USER CODE BEGIN EC */

/* USER CODE END EC */

/* Private variables ---------------------------------------------------------*/
/* USER CODE BEGIN PV */

/* USER CODE END PV */

/* Exported macro ------------------------------------------------------------*/
/* USER CODE BEGIN EM */

/* USER CODE END EM */

/* Private function prototypes -----------------------------------------------*/

/* USER CODE BEGIN PFP */

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

/* USER CODE END 0 */

UTIL_ADV_TRACE_Status_t UART_Init(  void (*cb)(void *))
{
  /* USER CODE BEGIN UART_Init 1 */

  /* USER CODE END UART_Init 1 */

  return UTIL_ADV_TRACE_UNKNOWN_ERROR;

  /* USER CODE BEGIN UART_Init 2 */

  /* USER CODE END UART_Init 2 */
}

UTIL_ADV_TRACE_Status_t UART_DeInit( void )
{
  /* USER CODE BEGIN UART_DeInit 1 */

  /* USER CODE END UART_DeInit 1 */

  /* USER CODE BEGIN UART_DeInit 2 */

  /* USER CODE END UART_DeInit 2 */

  return UTIL_ADV_TRACE_UNKNOWN_ERROR;
}

UTIL_ADV_TRACE_Status_t UART_StartRx(void (*cb)(uint8_t *pdata, uint16_t size, uint8_t error))
{
  /* USER CODE BEGIN UART_StartRx 1 */

  /* USER CODE END UART_StartRx 1 */

  /* USER CODE BEGIN UART_StartRx 2 */

  /* USER CODE END UART_StartRx 2 */

  return UTIL_ADV_TRACE_UNKNOWN_ERROR;
}

UTIL_ADV_TRACE_Status_t UART_TransmitDMA ( uint8_t *pdata, uint16_t size )
{
  /* USER CODE BEGIN UART_TransmitDMA 1 */

  /* USER CODE END UART_TransmitDMA 1 */

  /* USER CODE BEGIN UART_TransmitDMA 2 */

  /* USER CODE END UART_TransmitDMA 2 */

  return UTIL_ADV_TRACE_UNKNOWN_ERROR;
}

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 1 */

/* USER CODE END 1 */
