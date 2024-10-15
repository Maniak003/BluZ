/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
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
/* Includes ------------------------------------------------------------------*/
#include "main.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include "LTC1662.h"
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */

/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
ADC_HandleTypeDef hadc4;
DMA_NodeTypeDef Node_GPDMA1_Channel0;
DMA_QListTypeDef List_GPDMA1_Channel0;
DMA_HandleTypeDef handle_GPDMA1_Channel0;

CRC_HandleTypeDef hcrc;

LPTIM_HandleTypeDef hlptim1;
LPTIM_HandleTypeDef hlptim2;

RAMCFG_HandleTypeDef hramcfg_SRAM1;

RNG_HandleTypeDef hrng;

RTC_HandleTypeDef hrtc;

TIM_HandleTypeDef htim17;

UART_HandleTypeDef huart2;

/* USER CODE BEGIN PV */

bool connectFlag = false, LEDflag = false, SoundFlag = false, VibroFlag = false, autoStartSpecrometr = false;
uint32_t currentTimeAvg, pulseCounterAvg, interval1 = 0, interval2 = 0, interval3 = 0, interval4 = 0, intervalTmp = 0, pulseCounter = 0,  pulseCounterSecond = 0, pulseLevel[3] = {0}, currentTime = 0, CPS = 0;
char uartBuffer[400] = {0,};
/* Буфер для работы с flash */
uint64_t PL[8] = {0,};
uint8_t resolution = 0; /* 0 - 1024, 1 - 2048, 2 - 4096, 3 - Логи, 4 - Параметры, 5 - История дозиметра*/
uint8_t tmpBTBuffer[DATA_NOTIFICATION_MAX_PACKET_SIZE] = {0,};
uint16_t currTemterature, currVoltage, tmpSpecterBuffer[4096];
uint16_t dozimetrBuffer[SIZE_DOZIMETR_BUFER] = {0,};
int indexDozimetrBufer = 0;

/* Настройки прибора */
uint16_t specterBuffer[SIZE_BUF_4096] = {0,};
bool SoundEnable = true, VibroEnable = true, LEDEnable = false;		// Собровождение квантов
bool levelSound1 = true, levelSound2 = true, levelSound3 = true;	// Активность звука для разных уровней
bool levelVibro1 = true, levelVibro2 = true, levelVibro3 = true;	// Активность вибро для разнвх уровней
bool flagTemperatureMess = false;

/*
union dataC {
	float Float;
	uint8_t Uint[4];
};
*/

/* Коэффициенты для коррекции температуры */
float TK1, TK2;

int level1, level2, level3;											// Значения порогов
uint32_t level1_cps, level2_cps, level3_cps;						// Значения порогов в CPS
union dataC calcCoeff;												// Коэффициент для пересчета CPS в uR/h
union dataC enCoefA, enCoefB, enCoefC;								// Коэффициенты полинома для преобразования канала в энергию
union dataA Temperature, Voltage;									// Температура и напряжение батареи
union dataA AvgCPS;													// Средний CPS за время с последнего старта.

uint16_t HVoltage = 256, comparatorLevel = 256;						// Уровни настройки высокого напряжения и компаратора

uint8_t notifyFlags = 0;											// 1 - Звук, 2 - Вибро, 4 - Led

/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
void PeriphCommonClock_Config(void);
static void MX_GPIO_Init(void);
static void MX_GPDMA1_Init(void);
static void MX_ADC4_Init(void);
static void MX_USART2_UART_Init(void);
static void MX_CRC_Init(void);
static void MX_RAMCFG_Init(void);
static void MX_RNG_Init(void);
static void MX_ICACHE_Init(void);
static void MX_LPTIM2_Init(void);
static void MX_LPTIM1_Init(void);
static void MX_TIM17_Init(void);
/* USER CODE BEGIN PFP */

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

/*
 * Управление оповещениями
 *
 * SRC 		- SOUND_NOTIFY | VIBRO_NOTIFY | LED_NOTIFY
 * repCnt	- Количество повторов для звука и вибро
 *
 */
void NotifyAct(uint8_t SRC, uint32_t repCnt) {
	if (SoundEnable || VibroEnable ) {
		/* Установим количество повторов */
		hlptim1.Init.RepetitionCounter = repCnt;
		if (HAL_LPTIM_Init(&hlptim1) == HAL_OK) {
			notifyFlags = SRC;
			/* Включаем звук и вибро */
			HAL_LPTIM_OnePulse_Start_IT(&hlptim1, LPTIM_CHANNEL_1);
		}
	}
	if (LEDEnable) {
		/*
		if (SRC & LED_NOTIFY) {
			LEDflag = true;
			HAL_GPIO_WritePin(LED_GPIO_Port, LED_Pin, GPIO_PIN_SET);
			HAL_TIM_Base_Start_IT(&htim17);
		}*/
	}
}

void
/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{

  /* USER CODE BEGIN 1 */
	/* Настройка температурных коэффициентов */
	TK1 = (float) (TEMPSENSOR_CAL2_TEMP - TEMPSENSOR_CAL1_TEMP) / (float)(*TEMPSENSOR_CAL2_ADDR - *TEMPSENSOR_CAL1_ADDR);
	TK2 = (float) TEMPSENSOR_CAL1_TEMP - TK1 * (float) ( *(__IO uint16_t*) TEMPSENSOR_CAL1_ADDR);
	TK1 = TK1 * ADC_VREF / ((float)VREFINT_CAL_VREF / 1000.0f);

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();
  /* Config code for STM32_WPAN (HSE Tuning must be done before system clock configuration) */
  MX_APPE_Config();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

/* Configure the peripherals common clocks */
  PeriphCommonClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_GPDMA1_Init();
  MX_ADC4_Init();
  MX_USART2_UART_Init();
  MX_CRC_Init();
  MX_RAMCFG_Init();
  MX_RNG_Init();
  MX_RTC_Init();
  MX_ICACHE_Init();
  MX_LPTIM2_Init();
  MX_LPTIM1_Init();
  MX_TIM17_Init();
  /* USER CODE BEGIN 2 */
  //MX_GPIO_Init();
  HAL_ADCEx_Calibration_Start(&hadc4);

  bzero((char *) uartBuffer, sizeof(uartBuffer));
  sprintf(uartBuffer, "\n\rStart.\n\r");
  HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);

  /* USER CODE END 2 */

  /* Init code for STM32_WPAN */
  MX_APPE_Init(NULL);

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  //__//HAL_TIM_CLEAR_FLAG(&htim1, TIM_SR_UIF); // Clear flags


  //resolution = 0;			/* Тест разрешение 1024 */
  //resolution = 1;			/* Тест разрешение 2048 */
  //resolution = 2;			/* Тест разрешение 4096 */


  /* Включим LED, Vibro, Sound */
/*
  NotifyAct(
		 // SOUND_NOTIFY
		 VIBRO_NOTIFY
		//| LED_NOTIFY
		, 1);
*/

  //HAL_RTCEx_SetWakeUpTimer_IT(&hrtc, 2, WakeUpClock, WakeUpAutoClr)

  if (readFlash() == HAL_OK) {
	  bzero((char *) uartBuffer, sizeof(uartBuffer));
	  sprintf(uartBuffer, "\n\rRead flash Ok. Address: %lx\n\r", MAGIC_KEY_ADDRESS);
	  HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
  }
  /*
   * Настройка порога компаратора
   */
  setLevelOnPort(CHANNEL_A, comparatorLevel);

  /*
   * Настройка высокого напряжения
   * Чем выше значение тем ниже напряжение.
   */
  setLevelOnPort(CHANNEL_B, HVoltage);

  /* Запуск набора спектра при активном автостарте */
  if (autoStartSpecrometr) {
	  HAL_ADC_Start_DMA(&hadc4, pulseLevel, 3);
	  //__HAL_DMA_DISABLE_IT(hadc4.DMA_Handle, DMA_IT_HT);
	  hadc4.DMA_Handle->Instance->CCR &= ~DMA_IT_HT;
	  /* Включим ADC для одного канала */
	  MODIFY_REG(hadc4.Instance->CHSELR, ADC_CHSELR_SQ_ALL, ((ADC_CHSELR_SQ2 | ADC_CHSELR_SQ3 | ADC_CHSELR_SQ4 | ADC_CHSELR_SQ5 | ADC_CHSELR_SQ6 | ADC_CHSELR_SQ7 | ADC_CHSELR_SQ8) << (((1UL - 1UL) * ADC_REGULAR_RANK_2) & 0x1FUL)) | (hadc4.ADCGroupRegularSequencerRanks));
	  //hadc4.DMA_Handle &= ~DMA_IT_HT;
  }
  pulseCounter = 0;
  pulseCounterSecond = 0;
  currentTime = 0;

  /*
   * Значения заголовка и формат данных для передачи
   * 0,1,2 - Маркер начала <B>
   * 3 -  количество MTU
   *    1    - Параметры прибора
   *    3    - Даные лога
   *    4    - Данные дозиметра
   *    9    - Спектр 1024 разрядов
   *    17   - Спектр 2048 разрядов
   *    34   - Спектр 4096 разрядов
   *
   * 4 - Тип передаваемых данных
   *    0   - Текущий спектр
   *    1   - Исторический спектр
   *
   * 5, 6   - Общее число импульсов от начала измерения uint32_t
   * 7, 8   - Число импульсов за последнюю секунду. uint32_t
   * 9, 10  - Общее время в секундах uint32_t
   * 11, 12 - Среднее количество имльсов в секунду. float
   * 13, 14 - Температура в гр. цельсия
   * 15, 16 - Напряжение батареи в вольтах
   *
   */


  while (1)
  {
    /* USER CODE END WHILE */
    MX_APPE_Process();

    /* USER CODE BEGIN 3 */
    intervalTmp = HAL_GetTick();

	if (interval3 + INTERVAL3 < intervalTmp) {
		interval3 = intervalTmp;


	}


    if ( connectFlag && (interval2 + INTERVAL2 < intervalTmp) /*&& system_startup_done*/) {
	  interval2 = intervalTmp;

	  /* Шаблон заголовка */
	  uint8_t hdr[] = {'<', 'B', '>', 0, 0, 0, 0, 0};

	  uint16_t countMTU = 0;
	  uint16_t idxCS = 0 ;
	  switch (resolution) {
		  case 0: {
			  countMTU = NUMBER_MTU_1024;
			  idxCS = SIZE_BUF_1024 - 1 ;
			  hdr[4] = 0;					/* Тип передаваемых данных */
			  break;
		  }
		  case 1: {
			  countMTU = NUMBER_MTU_2048;
			  idxCS = SIZE_BUF_2048 - 1;
			  hdr[4] = 0;
			  break;
		  }
		  case 2: {
			  countMTU = NUMBER_MTU_4096;
			  idxCS = SIZE_BUF_4096 - 1;
			  hdr[4] = 0;
			  break;
		  }
		  case 3: {
			  countMTU = NUMBER_MTU_LOG;
			  idxCS = SIZE_BUF_LOG - 1;
			  hdr[4] = 0;
			  break;
		  }
		  case 4: {
			  countMTU = NUMBER_MTU_PARAM;
			  idxCS = SIZE_BUF_PARAM - 1;
			  hdr[4] = 0;
			  break;
		  }
		  case 5: {
			  countMTU = NUMBER_MTU_DOZR;
			  idxCS = SIZE_BUF_DOZR - 1;
			  hdr[4] = 0;
			  break;
		  }
	  }

	  /* Подготовка заголовка */
	  hdr[3] = countMTU;		/* Количество передач */
	  specterBuffer[0] = (uint16_t) (hdr[0] | (((uint16_t) hdr[1] << 8) & 0xFF00));
	  specterBuffer[1] = (uint16_t) (hdr[2] | (((uint16_t) hdr[3] << 8) & 0xFF00));
	  specterBuffer[2] = (uint16_t) (hdr[4] | (((uint16_t) hdr[5] << 8) & 0xFF00));
	  specterBuffer[3] = (uint16_t) (hdr[6] | (((uint16_t) hdr[7] << 8) & 0xFF00));

	  /* Общее количество импульсов */
	  specterBuffer[5] = pulseCounterAvg & 0xFFFF;
	  specterBuffer[6] = ((uint32_t) pulseCounterAvg >> 16 ) & 0xFFFF;

	  /* Импульсы за последнюю секунду */
	  specterBuffer[7] = CPS & 0xFFFF;
	  specterBuffer[8] = (CPS >> 16) & 0xFFFF;

	  /* Общее время в секундах от последнего старта */
	  specterBuffer[9] =  currentTimeAvg & 0xFFFF;
	  specterBuffer[10] = (currentTimeAvg >> 16) & 0xFFFF;

	  /* Среднее количество импульсов от последнего старта */
	  AvgCPS.Float = (float) pulseCounterAvg / (float) currentTimeAvg;
	  specterBuffer[11] = AvgCPS.Uint[0];
	  specterBuffer[12] = AvgCPS.Uint[1];

	 /*
	  *	Расчет температуры
	  *	 TS_CAL1 (30 °C VREF+ = 3.0 V)  0x0BF9 0710 - 0x0BF9 0711
	  *	 TS_CAL2 (130 °C VREF+ = 3.0 V) 0x0BF9 0742 - 0x0BF9 0743
	  *
	  *	 T(°C) = (TS_CAL2_TEMP – TS_CAL1_TEMP) / (TS_CAL2 – TS_CAL1) * ( TS_DATA – TS_CAL1 ) + TS_CAL1_TEMP
	  *
	  *	 T(°C) = (130 - 30) / (*(__IO uint32_t*) 0x0BF9 0742 - *(__IO uint32_t*) 0x0BF9 0710) * (TS_DATA - *(__IO uint32_t*) 0x0BF9 0710) + 30
	  */
	  Temperature.Float = TK1 * (float) currTemterature + TK2;
	  Voltage.Float = currVoltage * ADC_VREF_COEF;
	  specterBuffer[13] = Temperature.Uint[0];
	  specterBuffer[14] = Temperature.Uint[1];

	  /* Напряжение батареи */
	  specterBuffer[15] = Voltage.Uint[0];
	  specterBuffer[16] = Voltage.Uint[1];


	  //
	  uint16_t tmpCS = 0;			/* Очистим контрольнуюю сумму */
	  /*
	   * Загрузка спектра в буфер,
	   * что бы исключить изменение данных на время передачи
	   *
	   */
	  int kkk = 0;
	  for (int jjj = HEADER_OFFSET; jjj < idxCS; jjj++) {
		  specterBuffer[jjj] = tmpSpecterBuffer[kkk++];
		  /* Тестовые данные */
		  //specterBuffer[jjj] = 0;
		  //specterBuffer[jjj] = log(jjj) * 400;
	  }
	  //specterBuffer[2048] = 10;
	  //specterBuffer[idxCS - 1] = 1;
	  specterBuffer[idxCS] = 0;
	  //specterBuffer[SIZE_BUF_1024 - 1 ] = 0;
	  /* End test */
	  kkk = 0;
	  for (int iii = 0; iii < countMTU; iii++) {
		  if ( ! connectFlag) {
			  break;
		  }
		  for (int jjj = 0; jjj < MTUSizeValue; jjj++) {
			  uint16_t dataSpectr = specterBuffer[kkk++];
			  uint8_t tmpByte;
			  tmpByte  = (uint8_t) (dataSpectr & 0xFF);
			  tmpBTBuffer[jjj++] = tmpByte;
			  tmpCS = tmpCS + tmpByte;
			  tmpByte = (uint8_t) ((dataSpectr >> 8) & 0xFF);
			  tmpBTBuffer[jjj] = tmpByte;
			  tmpCS = tmpCS + tmpByte;
		  }
		  if (kkk >= idxCS) {
			  tmpBTBuffer[242] = (uint8_t) (tmpCS & 0xFF);
			  tmpBTBuffer[243] = (uint8_t) ((tmpCS >> 8) & 0xFF);
		  }
		  sendData(tmpBTBuffer);
		  MX_APPE_Process();
		  /*
		   * TODO -- требуется задержка в передаче, иначе не все пакеты принимаются
		   */
		  HAL_Delay(40);
			//bzero((char *) uartBuffer, sizeof(uartBuffer));
			//sprintf(uartBuffer, "MTU: %d\n\r", iii);
			//HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
	  }
		bzero((char *) uartBuffer, sizeof(uartBuffer));
		sprintf(uartBuffer, "CS: %d, MTU: %d\n\r", tmpCS, MTUSizeValue);
		HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
	}
    if (interval1 + INTERVAL1 < intervalTmp) {
    	interval1 = intervalTmp;
    	//HAL_GPIO_TogglePin(LED_GPIO_Port, LED_Pin);
    	//NotifyAct(SOUND_NOTIFY, 2);
		if (connectFlag) {
			//NotifyAct(LED_NOTIFY);

		}
    }

    /* Измерение напряжения батареи и температуры МК */
    if (interval4 + INTERVAL4 < intervalTmp) {
    	interval4 = intervalTmp;
    	if (! flagTemperatureMess) {
    		/* Включим ADC для трех каналов */
    		MODIFY_REG(hadc4.Instance->CHSELR, ADC_CHSELR_SQ_ALL, ((ADC_CHSELR_SQ2 | ADC_CHSELR_SQ3 | ADC_CHSELR_SQ4 | ADC_CHSELR_SQ5 | ADC_CHSELR_SQ6 | ADC_CHSELR_SQ7 | ADC_CHSELR_SQ8) << (((3UL - 1UL) * ADC_REGULAR_RANK_2) & 0x1FUL)) | (hadc4.ADCGroupRegularSequencerRanks));
			flagTemperatureMess = true;						// Для единичного измерения
    	}
    }
  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  /** Configure the main internal regulator output voltage
  */
  if (HAL_PWREx_ControlVoltageScaling(PWR_REGULATOR_VOLTAGE_SCALE2) != HAL_OK)
  {
    Error_Handler();
  }

  /** Configure LSE Drive Capability
  */
  HAL_PWR_EnableBkUpAccess();
  __HAL_RCC_LSEDRIVE_CONFIG(RCC_LSEDRIVE_MEDIUMLOW);

  /** Initializes the CPU, AHB and APB busses clocks
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSI|RCC_OSCILLATORTYPE_HSE
                              |RCC_OSCILLATORTYPE_LSE;
  RCC_OscInitStruct.HSEState = RCC_HSE_ON;
  RCC_OscInitStruct.HSEDiv = RCC_HSE_DIV2;
  RCC_OscInitStruct.LSEState = RCC_LSE_ON;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT;
  RCC_OscInitStruct.PLL1.PLLState = RCC_PLL_NONE;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB busses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2
                              |RCC_CLOCKTYPE_PCLK7|RCC_CLOCKTYPE_HCLK5;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_HSI;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV2;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB7CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.AHB5_PLL1_CLKDivider = RCC_SYSCLK_PLL1_DIV1;
  RCC_ClkInitStruct.AHB5_HSEHSI_CLKDivider = RCC_SYSCLK_HSEHSI_DIV2;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_0) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief Peripherals Common Clock Configuration
  * @retval None
  */
void PeriphCommonClock_Config(void)
{
  RCC_PeriphCLKInitTypeDef PeriphClkInit = {0};

  /** Initializes the peripherals clock
  */
  PeriphClkInit.PeriphClockSelection = RCC_PERIPHCLK_RADIOST;
  PeriphClkInit.RadioSlpTimClockSelection = RCC_RADIOSTCLKSOURCE_LSE;

  if (HAL_RCCEx_PeriphCLKConfig(&PeriphClkInit) != HAL_OK)
  {
    Error_Handler();
  }
}

/**
  * @brief ADC4 Initialization Function
  * @param None
  * @retval None
  */
static void MX_ADC4_Init(void)
{

  /* USER CODE BEGIN ADC4_Init 0 */

  /* USER CODE END ADC4_Init 0 */

  ADC_ChannelConfTypeDef sConfig = {0};

  /* USER CODE BEGIN ADC4_Init 1 */

  /* USER CODE END ADC4_Init 1 */

  /** Common config
  */
  hadc4.Instance = ADC4;
  hadc4.Init.ClockPrescaler = ADC_CLOCK_ASYNC_DIV1;
  hadc4.Init.Resolution = ADC_RESOLUTION_12B;
  hadc4.Init.DataAlign = ADC_DATAALIGN_RIGHT;
  hadc4.Init.ScanConvMode = ADC_SCAN_ENABLE;
  hadc4.Init.EOCSelection = ADC_EOC_SINGLE_CONV;
  hadc4.Init.LowPowerAutoPowerOff = DISABLE;
  hadc4.Init.LowPowerAutonomousDPD = ADC_LP_AUTONOMOUS_DPD_DISABLE;
  hadc4.Init.LowPowerAutoWait = DISABLE;
  hadc4.Init.ContinuousConvMode = DISABLE;
  hadc4.Init.NbrOfConversion = 3;
  hadc4.Init.DiscontinuousConvMode = ENABLE;
  hadc4.Init.ExternalTrigConv = ADC_EXTERNALTRIG_EXT_IT15;
  hadc4.Init.ExternalTrigConvEdge = ADC_EXTERNALTRIGCONVEDGE_RISING;
  hadc4.Init.DMAContinuousRequests = ENABLE;
  hadc4.Init.TriggerFrequencyMode = ADC_TRIGGER_FREQ_HIGH;
  hadc4.Init.Overrun = ADC_OVR_DATA_OVERWRITTEN;
  hadc4.Init.SamplingTimeCommon1 = ADC_SAMPLETIME_12CYCLES_5;
  hadc4.Init.SamplingTimeCommon2 = ADC_SAMPLETIME_814CYCLES_5;
  hadc4.Init.OversamplingMode = DISABLE;
  if (HAL_ADC_Init(&hadc4) != HAL_OK)
  {
    Error_Handler();
  }

  /** Configure Regular Channel
  */
  sConfig.Channel = ADC_CHANNEL_9;
  sConfig.Rank = ADC_REGULAR_RANK_1;
  sConfig.SamplingTime = ADC_SAMPLINGTIME_COMMON_1;
  if (HAL_ADC_ConfigChannel(&hadc4, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }

  /** Configure Regular Channel
  */
  sConfig.Channel = ADC_CHANNEL_7;
  sConfig.Rank = ADC_REGULAR_RANK_2;
  sConfig.SamplingTime = ADC_SAMPLINGTIME_COMMON_2;
  if (HAL_ADC_ConfigChannel(&hadc4, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }

  /** Configure Regular Channel
  */
  sConfig.Channel = ADC_CHANNEL_TEMPSENSOR;
  sConfig.Rank = ADC_REGULAR_RANK_3;
  if (HAL_ADC_ConfigChannel(&hadc4, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN ADC4_Init 2 */

  /* USER CODE END ADC4_Init 2 */

}

/**
  * @brief CRC Initialization Function
  * @param None
  * @retval None
  */
static void MX_CRC_Init(void)
{

  /* USER CODE BEGIN CRC_Init 0 */

  /* USER CODE END CRC_Init 0 */

  /* USER CODE BEGIN CRC_Init 1 */

  /* USER CODE END CRC_Init 1 */
  hcrc.Instance = CRC;
  hcrc.Init.DefaultPolynomialUse = DEFAULT_POLYNOMIAL_DISABLE;
  hcrc.Init.DefaultInitValueUse = DEFAULT_INIT_VALUE_ENABLE;
  hcrc.Init.GeneratingPolynomial = 7607;
  hcrc.Init.CRCLength = CRC_POLYLENGTH_16B;
  hcrc.Init.InputDataInversionMode = CRC_INPUTDATA_INVERSION_NONE;
  hcrc.Init.OutputDataInversionMode = CRC_OUTPUTDATA_INVERSION_DISABLE;
  hcrc.InputDataFormat = CRC_INPUTDATA_FORMAT_WORDS;
  if (HAL_CRC_Init(&hcrc) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN CRC_Init 2 */

  /* USER CODE END CRC_Init 2 */

}

/**
  * @brief GPDMA1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_GPDMA1_Init(void)
{

  /* USER CODE BEGIN GPDMA1_Init 0 */

  /* USER CODE END GPDMA1_Init 0 */

  /* Peripheral clock enable */
  __HAL_RCC_GPDMA1_CLK_ENABLE();

  /* GPDMA1 interrupt Init */
    HAL_NVIC_SetPriority(GPDMA1_Channel0_IRQn, 0, 0);
    HAL_NVIC_EnableIRQ(GPDMA1_Channel0_IRQn);

  /* USER CODE BEGIN GPDMA1_Init 1 */

  /* USER CODE END GPDMA1_Init 1 */
  /* USER CODE BEGIN GPDMA1_Init 2 */

  /* USER CODE END GPDMA1_Init 2 */

}

/**
  * @brief ICACHE Initialization Function
  * @param None
  * @retval None
  */
static void MX_ICACHE_Init(void)
{

  /* USER CODE BEGIN ICACHE_Init 0 */

  /* USER CODE END ICACHE_Init 0 */

  /* USER CODE BEGIN ICACHE_Init 1 */

  /* USER CODE END ICACHE_Init 1 */

  /** Enable instruction cache in 1-way (direct mapped cache)
  */
  if (HAL_ICACHE_ConfigAssociativityMode(ICACHE_1WAY) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_ICACHE_Enable() != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN ICACHE_Init 2 */

  /* USER CODE END ICACHE_Init 2 */

}

/**
  * @brief LPTIM1 Initialization Function
  * @param None
  * @retval None
  */
static void MX_LPTIM1_Init(void)
{

  /* USER CODE BEGIN LPTIM1_Init 0 */

  /* USER CODE END LPTIM1_Init 0 */

  LPTIM_OC_ConfigTypeDef sConfig1 = {0};

  /* USER CODE BEGIN LPTIM1_Init 1 */

  /* USER CODE END LPTIM1_Init 1 */
  hlptim1.Instance = LPTIM1;
  hlptim1.Init.Clock.Source = LPTIM_CLOCKSOURCE_APBCLOCK_LPOSC;
  hlptim1.Init.Clock.Prescaler = LPTIM_PRESCALER_DIV128;
  hlptim1.Init.Trigger.Source = LPTIM_TRIGSOURCE_SOFTWARE;
  hlptim1.Init.Period = 10000;
  hlptim1.Init.UpdateMode = LPTIM_UPDATE_IMMEDIATE;
  hlptim1.Init.CounterSource = LPTIM_COUNTERSOURCE_INTERNAL;
  hlptim1.Init.Input1Source = LPTIM_INPUT1SOURCE_GPIO;
  hlptim1.Init.Input2Source = LPTIM_INPUT2SOURCE_GPIO;
  hlptim1.Init.RepetitionCounter = 0;
  if (HAL_LPTIM_Init(&hlptim1) != HAL_OK)
  {
    Error_Handler();
  }
  sConfig1.Pulse = 4000;
  sConfig1.OCPolarity = LPTIM_OCPOLARITY_HIGH;
  if (HAL_LPTIM_OC_ConfigChannel(&hlptim1, &sConfig1, LPTIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN LPTIM1_Init 2 */

  /* USER CODE END LPTIM1_Init 2 */

}

/**
  * @brief LPTIM2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_LPTIM2_Init(void)
{

  /* USER CODE BEGIN LPTIM2_Init 0 */

  /* USER CODE END LPTIM2_Init 0 */

  LPTIM_OC_ConfigTypeDef sConfig1 = {0};

  /* USER CODE BEGIN LPTIM2_Init 1 */

  /* USER CODE END LPTIM2_Init 1 */
  hlptim2.Instance = LPTIM2;
  hlptim2.Init.Clock.Source = LPTIM_CLOCKSOURCE_APBCLOCK_LPOSC;
  hlptim2.Init.Clock.Prescaler = LPTIM_PRESCALER_DIV1;
  hlptim2.Init.Trigger.Source = LPTIM_TRIGSOURCE_SOFTWARE;
  hlptim2.Init.Period = 2540;
  hlptim2.Init.UpdateMode = LPTIM_UPDATE_IMMEDIATE;
  hlptim2.Init.CounterSource = LPTIM_COUNTERSOURCE_INTERNAL;
  hlptim2.Init.Input1Source = LPTIM_INPUT1SOURCE_GPIO;
  hlptim2.Init.Input2Source = LPTIM_INPUT2SOURCE_GPIO;
  hlptim2.Init.RepetitionCounter = 0;
  if (HAL_LPTIM_Init(&hlptim2) != HAL_OK)
  {
    Error_Handler();
  }
  sConfig1.Pulse = 1270;
  sConfig1.OCPolarity = LPTIM_OCPOLARITY_HIGH;
  if (HAL_LPTIM_OC_ConfigChannel(&hlptim2, &sConfig1, LPTIM_CHANNEL_2) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN LPTIM2_Init 2 */

  /* USER CODE END LPTIM2_Init 2 */
  HAL_LPTIM_MspPostInit(&hlptim2);

}

/**
  * @brief RAMCFG Initialization Function
  * @param None
  * @retval None
  */
static void MX_RAMCFG_Init(void)
{

  /* USER CODE BEGIN RAMCFG_Init 0 */

  /* USER CODE END RAMCFG_Init 0 */

  /* USER CODE BEGIN RAMCFG_Init 1 */

  /* USER CODE END RAMCFG_Init 1 */

  /** Initialize RAMCFG SRAM1
  */
  hramcfg_SRAM1.Instance = RAMCFG_SRAM1;
  if (HAL_RAMCFG_Init(&hramcfg_SRAM1) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN RAMCFG_Init 2 */

  /* USER CODE END RAMCFG_Init 2 */

}

/**
  * @brief RNG Initialization Function
  * @param None
  * @retval None
  */
static void MX_RNG_Init(void)
{

  /* USER CODE BEGIN RNG_Init 0 */

  /* USER CODE END RNG_Init 0 */

  /* USER CODE BEGIN RNG_Init 1 */

  /* USER CODE END RNG_Init 1 */
  hrng.Instance = RNG;
  hrng.Init.ClockErrorDetection = RNG_CED_ENABLE;
  if (HAL_RNG_Init(&hrng) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN RNG_Init 2 */

  /* USER CODE END RNG_Init 2 */

}

/**
  * @brief RTC Initialization Function
  * @param None
  * @retval None
  */
void MX_RTC_Init(void)
{

  /* USER CODE BEGIN RTC_Init 0 */

  /* USER CODE END RTC_Init 0 */

  RTC_PrivilegeStateTypeDef privilegeState = {0};

  /* USER CODE BEGIN RTC_Init 1 */

  /* USER CODE END RTC_Init 1 */

  /** Initialize RTC Only
  */
  hrtc.Instance = RTC;
  hrtc.Init.AsynchPrediv = 127;
  hrtc.Init.OutPut = RTC_OUTPUT_DISABLE;
  hrtc.Init.OutPutRemap = RTC_OUTPUT_REMAP_NONE;
  hrtc.Init.OutPutPolarity = RTC_OUTPUT_POLARITY_HIGH;
  hrtc.Init.OutPutType = RTC_OUTPUT_TYPE_OPENDRAIN;
  hrtc.Init.OutPutPullUp = RTC_OUTPUT_PULLUP_NONE;
  hrtc.Init.BinMode = RTC_BINARY_ONLY;
  if (HAL_RTC_Init(&hrtc) != HAL_OK)
  {
    Error_Handler();
  }
  privilegeState.rtcPrivilegeFull = RTC_PRIVILEGE_FULL_NO;
  privilegeState.backupRegisterPrivZone = RTC_PRIVILEGE_BKUP_ZONE_NONE;
  privilegeState.backupRegisterStartZone2 = RTC_BKP_DR0;
  privilegeState.backupRegisterStartZone3 = RTC_BKP_DR0;
  if (HAL_RTCEx_PrivilegeModeSet(&hrtc, &privilegeState) != HAL_OK)
  {
    Error_Handler();
  }

  /** Enable the WakeUp
  */
  if (HAL_RTCEx_SetWakeUpTimer_IT(&hrtc, 0, RTC_WAKEUPCLOCK_CK_SPRE_16BITS, 0) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN RTC_Init 2 */

  /* USER CODE END RTC_Init 2 */

}

/**
  * @brief TIM17 Initialization Function
  * @param None
  * @retval None
  */
static void MX_TIM17_Init(void)
{

  /* USER CODE BEGIN TIM17_Init 0 */

  /* USER CODE END TIM17_Init 0 */

  TIM_OC_InitTypeDef sConfigOC = {0};
  TIM_BreakDeadTimeConfigTypeDef sBreakDeadTimeConfig = {0};

  /* USER CODE BEGIN TIM17_Init 1 */

  /* USER CODE END TIM17_Init 1 */
  htim17.Instance = TIM17;
  htim17.Init.Prescaler = 15;
  htim17.Init.CounterMode = TIM_COUNTERMODE_UP;
  htim17.Init.Period = 65535;
  htim17.Init.ClockDivision = TIM_CLOCKDIVISION_DIV1;
  htim17.Init.RepetitionCounter = 0;
  htim17.Init.AutoReloadPreload = TIM_AUTORELOAD_PRELOAD_ENABLE;
  if (HAL_TIM_Base_Init(&htim17) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_OC_Init(&htim17) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_TIM_OnePulse_Init(&htim17, TIM_OPMODE_SINGLE) != HAL_OK)
  {
    Error_Handler();
  }
  sConfigOC.OCMode = TIM_OCMODE_TOGGLE;
  sConfigOC.Pulse = 0;
  sConfigOC.OCPolarity = TIM_OCPOLARITY_HIGH;
  sConfigOC.OCNPolarity = TIM_OCNPOLARITY_HIGH;
  sConfigOC.OCFastMode = TIM_OCFAST_DISABLE;
  sConfigOC.OCIdleState = TIM_OCIDLESTATE_RESET;
  sConfigOC.OCNIdleState = TIM_OCNIDLESTATE_RESET;
  if (HAL_TIM_OC_ConfigChannel(&htim17, &sConfigOC, TIM_CHANNEL_1) != HAL_OK)
  {
    Error_Handler();
  }
  sBreakDeadTimeConfig.OffStateRunMode = TIM_OSSR_DISABLE;
  sBreakDeadTimeConfig.OffStateIDLEMode = TIM_OSSI_DISABLE;
  sBreakDeadTimeConfig.LockLevel = TIM_LOCKLEVEL_OFF;
  sBreakDeadTimeConfig.DeadTime = 0;
  sBreakDeadTimeConfig.BreakState = TIM_BREAK_DISABLE;
  sBreakDeadTimeConfig.BreakPolarity = TIM_BREAKPOLARITY_HIGH;
  sBreakDeadTimeConfig.BreakFilter = 0;
  sBreakDeadTimeConfig.AutomaticOutput = TIM_AUTOMATICOUTPUT_DISABLE;
  if (HAL_TIMEx_ConfigBreakDeadTime(&htim17, &sBreakDeadTimeConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN TIM17_Init 2 */

  /* USER CODE END TIM17_Init 2 */

}

/**
  * @brief USART2 Initialization Function
  * @param None
  * @retval None
  */
static void MX_USART2_UART_Init(void)
{

  /* USER CODE BEGIN USART2_Init 0 */

  /* USER CODE END USART2_Init 0 */

  /* USER CODE BEGIN USART2_Init 1 */

  /* USER CODE END USART2_Init 1 */
  huart2.Instance = USART2;
  huart2.Init.BaudRate = 115200;
  huart2.Init.WordLength = UART_WORDLENGTH_8B;
  huart2.Init.StopBits = UART_STOPBITS_1;
  huart2.Init.Parity = UART_PARITY_NONE;
  huart2.Init.Mode = UART_MODE_TX;
  huart2.Init.HwFlowCtl = UART_HWCONTROL_NONE;
  huart2.Init.OverSampling = UART_OVERSAMPLING_8;
  huart2.Init.OneBitSampling = UART_ONE_BIT_SAMPLE_DISABLE;
  huart2.Init.ClockPrescaler = UART_PRESCALER_DIV1;
  huart2.AdvancedInit.AdvFeatureInit = UART_ADVFEATURE_RXOVERRUNDISABLE_INIT|UART_ADVFEATURE_DMADISABLEONERROR_INIT;
  huart2.AdvancedInit.OverrunDisable = UART_ADVFEATURE_OVERRUN_DISABLE;
  huart2.AdvancedInit.DMADisableonRxError = UART_ADVFEATURE_DMA_DISABLEONRXERROR;
  if (HAL_HalfDuplex_Init(&huart2) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_UARTEx_SetTxFifoThreshold(&huart2, UART_TXFIFO_THRESHOLD_1_8) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_UARTEx_SetRxFifoThreshold(&huart2, UART_RXFIFO_THRESHOLD_1_8) != HAL_OK)
  {
    Error_Handler();
  }
  if (HAL_UARTEx_EnableFifoMode(&huart2) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN USART2_Init 2 */

  /* USER CODE END USART2_Init 2 */

}

/**
  * @brief GPIO Initialization Function
  * @param None
  * @retval None
  */
static void MX_GPIO_Init(void)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};
/* USER CODE BEGIN MX_GPIO_Init_1 */
/* USER CODE END MX_GPIO_Init_1 */

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOB_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOC_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(A_DATA_GPIO_Port, A_DATA_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOA, A_SCK_Pin|A_CS_Pin|VIBRO_Pin|LED_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin : A_DATA_Pin */
  GPIO_InitStruct.Pin = A_DATA_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(A_DATA_GPIO_Port, &GPIO_InitStruct);

  /*Configure GPIO pins : A_SCK_Pin A_CS_Pin VIBRO_Pin LED_Pin */
  GPIO_InitStruct.Pin = A_SCK_Pin|A_CS_Pin|VIBRO_Pin|LED_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

  /*Configure GPIO pin : Sync_Pin */
  GPIO_InitStruct.Pin = Sync_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_IT_RISING;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  HAL_GPIO_Init(Sync_GPIO_Port, &GPIO_InitStruct);

  /*RT DEBUG GPIO_Init */
  RT_DEBUG_GPIO_Init();

  /* EXTI interrupt init*/
  HAL_NVIC_SetPriority(EXTI15_IRQn, 0, 0);
  HAL_NVIC_EnableIRQ(EXTI15_IRQn);

/* USER CODE BEGIN MX_GPIO_Init_2 */
/* USER CODE END MX_GPIO_Init_2 */
}

/* USER CODE BEGIN 4 */
void HAL_LPTIM_CompareMatchCallback(LPTIM_HandleTypeDef *hlptim) {
	if (hlptim->Channel == HAL_LPTIM_ACTIVE_CHANNEL_1) {
		if (LEDflag) {
		  LEDflag = false;
		  HAL_GPIO_WritePin(LED_GPIO_Port, LED_Pin, GPIO_PIN_RESET);
		}

		/* Включение звука */
		if (SoundEnable) {
			if (notifyFlags & SOUND_NOTIFY) {
				SoundFlag = true;
				HAL_LPTIM_PWM_Start(&hlptim2, LPTIM_CHANNEL_2);
			}
		}

		/* Включение вибро */
		if (VibroEnable) {
			if (notifyFlags & VIBRO_NOTIFY) {
				VibroFlag = true;
				HAL_GPIO_WritePin(VIBRO_GPIO_Port, VIBRO_Pin, GPIO_PIN_SET);
			}
		}
	}
}

void HAL_LPTIM_AutoReloadMatchCallback(LPTIM_HandleTypeDef *hlptim) {

	/* Отключание звука */
	if (SoundFlag) {
	  SoundFlag = false;
	  HAL_LPTIM_PWM_Stop(&hlptim2, LPTIM_CHANNEL_2);
	}

	/* Отключение вибро */
	if (VibroFlag) {
	  VibroFlag = false;
	  HAL_GPIO_WritePin(VIBRO_GPIO_Port, VIBRO_Pin, GPIO_PIN_RESET);
	}
}

/*
 *  Последнее событие в работе таймера
 */
void HAL_LPTIM_UpdateEventCallback(LPTIM_HandleTypeDef *hlptim) {

		/* На всякий случай дополнительно выключим звук и вибро */
		HAL_LPTIM_OnePulse_Stop_IT(&hlptim1, LPTIM_CHANNEL_1);
		HAL_LPTIM_PWM_Stop(&hlptim2, LPTIM_CHANNEL_2);
		HAL_GPIO_WritePin(VIBRO_GPIO_Port, VIBRO_Pin, GPIO_PIN_RESET);
}
/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}

#ifdef  USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */
