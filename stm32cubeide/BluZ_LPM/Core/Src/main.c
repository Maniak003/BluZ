/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
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
/* Includes ------------------------------------------------------------------*/
#include "main.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
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

LPTIM_HandleTypeDef hlptim2;

RAMCFG_HandleTypeDef hramcfg_SRAM1;

RNG_HandleTypeDef hrng;

RTC_HandleTypeDef hrtc;

/* USER CODE BEGIN PV */
bool historyRequest = false, connectFlag = false, LEDflag = false, SoundFlag = false, VibroFlag = false, autoStartSpecrometr = false, firstInital = true/*, findDevice = false*/;
uint32_t currentLevel = 10, tmp_level, currentTimeAvg, pulseCounterAvg, interval1 = 0, interval2 = 0, interval3 = 0, interval4 = 0, intervalNow = 0;
uint32_t tmpLevel, pulseCounter = 0,  pulseCounterSecond = 0, currentTime = 0, CPS = 0, TVLevel[3] = {0,}, spectrometerTime = 0, spectrometerPulse = 0;
uint16_t dozimetrBuffer[SIZE_DOZIMETR_BUFER] = {0,};
/*
 *	Точность измерения дозиметра
 *	1 sigma <10% -  100
 *	2 sigma <10% -  400
 *	3 sigma <10% -  900
 *	4 sigma <10% - 1600
 */
uint16_t dozimetrAquracy = DEFAULTAQR, edgeCounter = 0;
uint32_t aquracyInterval;
#ifdef MEDIAN
uint32_t MA[3];
uint16_t medianIdx;
#endif
int indexDozimetrBufer = 0;
int logIndex = 0;	/* Текущий указатель на буфер лога */
char uartBuffer[400] = {0,};
struct LG logBuffer[LOG_BUFER_SIZE];
/* Буфер для работы с flash */
uint64_t PL[18] = {0,};
spectrResolution_t resolutionSpecter = resolution1024;
uint8_t repetionCount = 0; /* 0 - 1024, 1 - 2048, 2 - 4096 */
uint8_t bitsOfChannal;	// Разрядность канала
/*
 * dataType
 * 0 - Дозиметр и логи,
 * 1 - Дозиметр, логи и спектр 1024,
 * 2 - Дозиметр, логи и спектр 2048,
 * 3 - Дозиметр, логи и спектр 4096,
 * 4 - Дозиметр, логи и история 1024
 * 5 - Дозиметр, логи и история 2048
 * 6 - Дозиметр, логи и история 4096
 */
sendDataTypes_t dataType = onlyDozimeter;
uint8_t tmpBTBuffer[DATA_NOTIFICATION_MAX_PACKET_SIZE] = {0,};
uint16_t currTemperature = 0, currVoltage = 0;
uint32_t tmpSpecterBuffer[MAX_RESOLUTION];
uint32_t historySpecterBuffer[MAX_RESOLUTION];

/* Настройки прибора */
uint16_t transmitBuffer[NUMBER_MTU_4096 * 244 / 2 + SPECTER_OFFSET] = {0,};
bool SoundEnable = true, VibroEnable = true, LEDEnable = false;		// Сопровождение квантов
bool levelSound1 = true, levelSound2 = true, levelSound3 = true;	// Активность звука для разных уровней
bool levelVibro1 = true, levelVibro2 = true, levelVibro3 = true;	// Активность вибро для разнвх уровней
bool startSpectrometr = false;
bool vibroFlag = false, soundFlag = false;
bool history_active = false; // Флаг набора истории.
bool overloadFlag = false;	// Флаг перегрузки

double CoefChan;							/* Коэффициент, для отказа от операции деления 65535 / 20 */
uint32_t limitChan;

/* Коэффициенты для коррекции температуры */
float TK1, TK2;

uint16_t level1, level2, level3;											// Значения порогов
uint32_t level1_cps, level2_cps, level3_cps;						// Значения порогов в CPS
union dataC calcCoeff;												// Коэффициент для пересчета CPS в uR/h
union dataC enCoefA1024, enCoefB1024, enCoefC1024;					// Коэффициенты полинома для преобразования канала в энергию для 1024
union dataC enCoefA2048, enCoefB2048, enCoefC2048;					// Коэффициенты полинома для преобразования канала в энергию для 2048
union dataC enCoefA4096, enCoefB4096, enCoefC4096;					// Коэффициенты полинома для преобразования канала в энергию для 4096
union dataA Temperature, Voltage;									// Температура и напряжение батареи
union dataA AvgCPS;													// Средний CPS за время с последнего старта.

uint16_t HVoltage = 256, comparatorLevel = 256;						// Уровни настройки высокого напряжения и компаратора

uint8_t notifyFlags = 0;											// 1 - Звук, 2 - Вибро, 4 - Led

UTIL_TIMER_Object_t timerMeasureInterval, timerVibro, timerVibroOff, timerLed;
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
void PeriphCommonClock_Config(void);
static void SystemPower_Config(void);
/* USER CODE BEGIN PFP */
void updateMesurment(void);
void updateMesurmentCb(void *arg);
void TVMeasure(void);
void tempVoltADCInit(void);
void vibroActivate(void);
void updateVibroCb(void *arg);
void vibroActivateOff(void);
void updateVibroOffCb(void *arg);
void updateLedCb (void *arg);
void ledActivate(void);

/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

/*
 *	SamplingTime selector
 *	Определение времени выборки ADC
 *
 */
uint8_t currentSamplingTime = 1;
uint32_t samplingTm(uint8_t smplTm) {
	switch(smplTm) {
		case 0: return ADC_SAMPLETIME_1CYCLE_5;
		case 1: return ADC_SAMPLETIME_3CYCLES_5;
		case 2: return ADC_SAMPLETIME_7CYCLES_5;
		case 3: return ADC_SAMPLETIME_12CYCLES_5;
		case 4: return ADC_SAMPLETIME_19CYCLES_5;
		case 5: return ADC_SAMPLETIME_39CYCLES_5;
		case 6: return ADC_SAMPLETIME_79CYCLES_5;
		case 7: return ADC_SAMPLETIME_814CYCLES_5;
		default: return ADC_SAMPLETIME_1CYCLE_5;
	}
}

/*
 *	Добавление записи в лог
 *
 *	act
 *	0 -  Отсутствие события
 *	1 -  Включение прибора
 *	2 -  превышение уровня 1
 *	3 -  превышение уровня 2
 *	4 -  превышение уровня 3
 *	5 -  нормальный уровень
 *	6 -  Сброс дозиметра
 *	7 -  Сброс спектрометра
 *	8 -  Запись данных во флаш
 *	9 -  Запуск набора спектра
 *	10 - Останов набора спектра
 *	11 - Очистка лога
 *	12 - Сброс спектрометра при переключении разрешения
 *	13 - Изменение разрядности
 */


void logUpdate(logTypes_t act) {
	logBuffer[logIndex].time = currentTime;
	logBuffer[logIndex++].type = act;
	if (logIndex >= LOG_BUFER_SIZE) {		// Переполнение лога
		logIndex = 0;						// Начинаем с начала
	}
}
/*
 * Управление оповещениями
 *
 * SRC 		- SOUND_NOTIFY | VIBRO_NOTIFY | LED_NOTIFY
 * repCnt	- Количество повторов для звука и вибро
 *
 */
void NotifyAct(uint8_t SRC, uint32_t repCnt) {
	//if (SoundEnable || VibroEnable || findDevice) {
		/* Аквация вибро и звука */
		if (SRC & (SOUND_NOTIFY | VIBRO_NOTIFY)) {
			if (SRC & SOUND_NOTIFY) {
				soundFlag = true;
			}
			if (SRC & VIBRO_NOTIFY) {
				vibroFlag = true;
			}
		/* Установим количество повторов */
			repetionCount = repCnt;
			UTIL_TIMER_Start(&(timerVibro));
		}
		//findDevice = false;
	//}

	if (LEDEnable) {
		if (SRC & LED_NOTIFY) {
			LEDflag = true;
			//GPIO_InitTypeDef GPIO_InitStruct = {0};
			 // GPIO_InitStruct.Pin = A_SCK_Pin|A_CS_Pin|VIBRO_Pin|LED_Pin;
			 // GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
			 // GPIO_InitStruct.Pull = GPIO_NOPULL;
			 // GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
			 // HAL_GPIO_Init(GPIOA, &GPIO_InitStruct);

			HAL_GPIO_WritePin(LED_GPIO_Port, LED_Pin, GPIO_PIN_SET);
			UTIL_TIMER_Start(&(timerLed));
		}
	}
}

void calcPulseLevel() {
	level1_cps = level1 / calcCoeff.Float;
	level2_cps = level2 / calcCoeff.Float;
	level3_cps = level3 / calcCoeff.Float;
}
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
	TK2 = (float) TEMPSENSOR_CAL1_TEMP - TK1 * (float) *TEMPSENSOR_CAL1_ADDR;
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

  /* Configure the System Power */
  SystemPower_Config();

  /* USER CODE BEGIN SysInit */
  dataType = onlyDozimeter;

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_GPDMA1_Init();
  MX_ADC4_Init();
  MX_ICACHE_Init();
  MX_RAMCFG_Init();
  MX_RNG_Init();
  MX_RTC_Init();
  MX_LPTIM2_Init();
  /* USER CODE BEGIN 2 */
  //NotifyAct(SOUND_NOTIFY, 2);
  //HAL_Delay(1000);
  logUpdate(powerOnLog);	// Добавляем запись в логе о начале работы.
  HAL_ADCEx_Calibration_Start(&hadc4);
  /* USER CODE END 2 */

  /* Init code for STM32_WPAN */
  MX_APPE_Init(NULL);

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  if (readFlash() == HAL_OK) {}
  if (autoStartSpecrometr) {
	  switch (resolutionSpecter) {
	  case resolution1024:
		  dataType = dozimeterSpecter1024;
		  break;
	  case resolution2048:
		  dataType = dozimeterSpecter2048;
		  break;
	  case resolution4096:
		  dataType = dozimeterSpecter4096;
		  break;
	  default:
		  dataType = dozimeterSpecter1024;
		  resolutionSpecter = resolution1024;
		  break;
	  }
  } else {
	  dataType = onlyDozimeter;
  }
  /* Пересчет уровней в uint32_t для ускорения обработки */
  calcPulseLevel();
  CoefChan = 65535.0 / (double) bitsOfChannal;
  limitChan = (1 << bitsOfChannal) - 1 ;

  /*
   * Настройка порога компаратора
   */
  setLevelOnPort(CHANNEL_A, comparatorLevel);

  /*
   * Настройка высокого напряжения
   * Чем выше значение тем ниже напряжение.
   */
  setLevelOnPort(CHANNEL_B, HVoltage);
  //setLevelOnPort(CHANNEL_B, 200);

  /* Запуск набора спектра при активном автостарте */
  if (dataType > onlyDozimeter) {
	  //UTIL_LPM_SetStopMode(1U << CFG_LPM_LOG, UTIL_LPM_DISABLE);
	  //if (autoStartSpecrometr) {
		  //HAL_ADC_Start_DMA(&hadc4, TVLevel, 1);
		  logUpdate(startSpectrometerLog);
		  //hadc4.DMA_Handle->Instance->CCR &= ~DMA_IT_HT;
		  /* Включим ADC для одного канала */
		  //MODIFY_REG(hadc4.Instance->CHSELR, ADC_CHSELR_SQ_ALL, ((ADC_CHSELR_SQ2 | ADC_CHSELR_SQ3 | ADC_CHSELR_SQ4 | ADC_CHSELR_SQ5 | ADC_CHSELR_SQ6 | ADC_CHSELR_SQ7 | ADC_CHSELR_SQ8) << (((1UL - 1UL) * ADC_REGULAR_RANK_2) & 0x1FUL)) | (hadc4.ADCGroupRegularSequencerRanks));
	  //} else {
		  //tempVoltADCInit();
	  //}
  }

  /*
   * Значения заголовка и формат данных для передачи
   *
   * Управляющие данные uint8_t
   * 0,1,2 - Маркер начала <B>
   * 3 -  Тип передачи
   *    0    - Данные дозиметра и лог (6 MTU)
   *    1    - Данные дозиметра, лог и спектр 1024 (14 MTU)
   *    2    - Данные дозиметра, лог и спектр 2048 (23 MTU)
   *    3    - Данные дозиметра, лог и спектр 4096 (39 MTU)
   *    4	 - Данные дозиметра, лог и исторический спектр 1024
   *    5	 - Данные дозиметра, лог и исторический спектр 2048
   *    6	 - Данные дозиметра, лог и исторический спектр 4096
   * 4,5,6,7 - Зарезервировано
   *
   * Статистика и когфигурация uint16_t
   *
   * 5, 6   - Общее число импульсов от начала измерения uint32_t
   * 7, 8   - Число импульсов за последнюю секунду. uint32_t
   * 9, 10  - Общее время в секундах uint32_t
   * 11, 12 - Среднее количество имльсов в секунду. float
   * 13, 14 - Температура в гр. цельсия
   * 15, 16 - Напряжение батареи в вольтах
   * 17, 18 - Коэффициент полинома A для 1024 каналов
   * 19, 20 - Коэффициент полинома B для 1024 каналов
   * 21, 22 - Коэффициент полинома C для 1024 каналов
   * 23, 24 - Коэффициент пересчета uRh/cps
   * 25     - Значение высокого напряжения
   * 26     - Значение порога компаратора
   * 27     - Уровень первого порога
   * 28     - Уровень второго порога
   * 29     - Уровень третьего порога
   * 30     - Битовая конфигурация
   *     0 - Индикация кванта светодиодом
   *     1 - Индикация кванта звуком
   *     2 - Звук первого уровня
   *     3 - Звук второго уровня
   *     4 - Звук третьего уровня
   *     5 - Вибро первого уровня
   *     6 - Вибро второго уровня
   *     7 - Вибро третьего уровня
   *     8 - Автозапуск спектрометра при включении
   * 31, 32 - Коэффициент полинома A для 2048 каналов
   * 33, 34 - Коэффициент полинома B для 2048 каналов
   * 35, 36 - Коэффициент полинома C для 2048 каналов
   * 37, 38 - Коэффициент полинома A для 4096 каналов
   * 39, 40 - Коэффициент полинома B для 4096 каналов
   * 41, 42 - Коэффициент полинома C для 4096 каналов
   * 43, 44 - Время работы спектрометра в секундах
   * 45, 46 - Количество импульсов в спектре
   * 47		- Точность расчета для дозиметра, количество импульсов для усреднения.
   * 48(L)	- Разрядность канала 0x7
   * 48(L)	- Перегрузка 0x8 - 1 бит
   *
   * 50  - Данные дозиметра
   * 572 - Данные лога
   * 652 - Данные спектра
   *
   * 652 или 1676 или 2700 или 4748 - Контрольная сумма.
   *
   */

  /* Таймер для расчета статистики измерений */
  UTIL_SEQ_RegTask(1<<CFG_TASK_MEASURE_REQ_ID, UTIL_SEQ_RFU, updateMesurment);
  UTIL_TIMER_Create(&(timerMeasureInterval), MEASURE_INTERVAL, UTIL_TIMER_PERIODIC, &updateMesurmentCb, 0);
  UTIL_TIMER_Start(&(timerMeasureInterval));


  /* Таймер для LED */
  UTIL_SEQ_RegTask(1<<CFG_TASK_LED_REQ_ID, UTIL_SEQ_RFU, ledActivate);
  UTIL_TIMER_Create(&(timerLed), LED_PERIOD, UTIL_TIMER_ONESHOT, &updateLedCb, 0);

  /* Таймеры для управления вибро
   *	 CFG_TASK_VIBRO_REQ_ID -- задает интервал между событиями
   *	 CFG_TASK_OFFVIBRO_REQ_ID -- задает врямя работы вибро
   *
   */
  UTIL_SEQ_RegTask(1<<CFG_TASK_VIBRO_REQ_ID, UTIL_SEQ_RFU, vibroActivate);
  UTIL_TIMER_Create(&(timerVibro), VIBRO_PERIOD, UTIL_TIMER_ONESHOT, &updateVibroCb, 0);

  UTIL_SEQ_RegTask(1<<CFG_TASK_VIBROOFF_REQ_ID, UTIL_SEQ_RFU, vibroActivateOff);
  UTIL_TIMER_Create(&(timerVibroOff), VIBRO_TIME, UTIL_TIMER_ONESHOT, &updateVibroOffCb, 0);

  /* Включим Sound */
  //findDevice = true;
  NotifyAct(SOUND_NOTIFY /*| VIBRO_NOTIFY*/, 1);

  TVMeasure();

  /* Сброс счетчиков */
  pulseCounter = 0;
  pulseCounterSecond = 0;
  currentTime = 0;
  CPS = 0;

  while (1)
  {
    /* USER CODE END WHILE */
    MX_APPE_Process();

    /* USER CODE BEGIN 3 */
    if ( connectFlag && (interval2 < intervalNow) /*&& system_startup_done*/) {
	  interval2 = intervalNow + INTERVAL2;

	  //HAL_GPIO_TogglePin(LED_GPIO_Port, LED_Pin);
	  /* Шаблон заголовка */
	  uint16_t countMTU = 4;
	  sendDataTypes_t tmpDataType;
	  tmpDataType = dataType;
	  if (historyRequest) {
		  	  historyRequest = false;
			switch (resolutionSpecter) {
			case resolution1024:
				dataType = dozimeterHistory1024;
				break;
			case resolution2048:
				dataType = dozimeterHistory2048;
				break;
			case resolution4096:
				dataType = dozimeterHistory4096;
				break;
			default:
				dataType = dozimeterHistory1024;
				resolutionSpecter = resolution1024;
				break;
			}
	  }

	  transmitBuffer[0] = ((uint16_t) '<' & 0xFF) | (((uint16_t)'B' << 8) & 0xFF00);
	  transmitBuffer[1] = ((uint16_t) '>' & 0xFF) | ((dataType << 8) & 0xFF00);
	  transmitBuffer[2] = 0;
	  transmitBuffer[3] = 0;

	  uint16_t idxCS = 0 ;
	  int kkk = 0;
	  uint16_t nm_channel = 0;
	  switch (dataType) {
  	  case onlyDozimeter:										/* Передача данных дозиметра и лога */
  		  countMTU = NUMBER_MTU_DOZR;
  		  idxCS =  NUMBER_MTU_DOZR * 244 / 2 - 1;
  		  nm_channel = 0;
  		  break;
  	  case dozimeterSpecter1024:
  		  countMTU = NUMBER_MTU_1024;				/* Передача данных дозиметра, лога и спектра 1024 */
  		  idxCS = NUMBER_MTU_1024 * 244 / 2 - 1;
  		  nm_channel = CHANNELS_1024;
  		  break;
  	  case dozimeterSpecter2048:
  		  countMTU = NUMBER_MTU_2048;				/* Передача данных дозиметра, лога и спектра 2048 */
  		  idxCS = NUMBER_MTU_2048 * 244 / 2 - 1;
  		  nm_channel = CHANNELS_2048;
  		  break;
  	  case dozimeterSpecter4096:
  		  countMTU = NUMBER_MTU_4096;				/* Передача данных дозиметра, лога и спектра 4096 */
  		  idxCS = NUMBER_MTU_4096 * 244 / 2 - 1;
  		  nm_channel = CHANNELS_4096;
  		  break;
  	  case dozimeterHistory1024:
  		  countMTU = NUMBER_MTU_1024;				/* Передача данных дозиметра, лога и спектра 1024 */
  		  idxCS = NUMBER_MTU_1024 * 244 / 2 - 1;
  		  nm_channel = CHANNELS_1024;
  		  break;
  	  case dozimeterHistory2048:
  		  countMTU = NUMBER_MTU_2048;				/* Передача данных дозиметра, лога и спектра 2048 */
  		  idxCS = NUMBER_MTU_2048 * 244 / 2 - 1;
  		  nm_channel = CHANNELS_2048;
  		  break;
  	  case dozimeterHistory4096:
  		  countMTU = NUMBER_MTU_4096;				/* Передача данных дозиметра, лога и спектра 4096 */
  		  idxCS = NUMBER_MTU_4096 * 244 / 2 - 1;
  		  nm_channel = CHANNELS_4096;
  		  break;
	  }
	  if (dataType > onlyDozimeter) {							/* Нужно передавать спектр ? */
		  /* Test */
		  //for (int jjj = 0; jjj < nm_channel; jjj++) {
			//  tmpSpecterBuffer[jjj] = jjj * 100;
		  //}
		  /* Медианный фильтр для устранения артефактов ADC */
		#ifdef MEDIAN
		  MA[0] = 0;
		  MA[1] = 0;
		  MA[2] = 0;
		  medianIdx = 0;
		#endif
		  double tmpLog;
		  uint32_t tmpSpectrData;
		  for (int jjj = 0; jjj < nm_channel; jjj++) {
			  /* Что будем передавать, историю или текущий спектр */
			  if (dataType > dozimeterSpecter4096) {
				  tmpSpectrData = historySpecterBuffer[kkk++];
			  } else {
				  tmpSpectrData = tmpSpecterBuffer[kkk++];
			  }
			#ifdef MEDIAN
			  MA[medianIdx] = tmpSpectrData;
			  tmpSpectrData = (MA[0] < MA[1]) ? ((MA[1] < MA[2]) ? MA[1] : ((MA[2] < MA[0]) ? MA[0] : MA[2])) : ((MA[0] < MA[2]) ? MA[0] : ((MA[2] < MA[1]) ? MA[1] : MA[2]));
			  if (++medianIdx >= 3) {
				  medianIdx = 0;
			  }
			#endif
			  /* Логарифмическое сжатие */
			  tmpLog = log2(tmpSpectrData + 1) * (double) CoefChan;
			  transmitBuffer[jjj + SPECTER_OFFSET] = (uint16_t) tmpLog;
		  }
	  }
	  dataType = tmpDataType;	// Востановление исходного типа данных после передачи истории
		  /* Подготавливаем данные дозиметра */
	  uint16_t ddd = indexDozimetrBufer;
	  for (uint16_t jjj = HEADER_OFFSET; jjj < HEADER_OFFSET + SIZE_DOZIMETR_BUFER; jjj++) {
		  transmitBuffer[jjj] = dozimetrBuffer[ddd++];
		  if (ddd >= SIZE_DOZIMETR_BUFER) {
			  ddd = 0;
		  }
	  }
	  /* Test дозиметра*/
	  //for (uint16_t jjj = HEADER_OFFSET; jjj < HEADER_OFFSET + SIZE_DOZIMETR_BUFER; jjj++) {
		//transmitBuffer[jjj] = jjj;
	  //}

	  /* Подготавливаем данные лога */
	  uint16_t nnn;
	  nnn = LOG_OFFSET;
	  ddd = logIndex;
	  for (uint16_t jjj = 0; jjj < LOG_BUFER_SIZE; jjj++) {
		  transmitBuffer[nnn++] = logBuffer[ddd].time & 0xFFFF;
		  transmitBuffer[nnn++] = (logBuffer[ddd].time >> 16) & 0xFFFF;
		  transmitBuffer[nnn++] = (uint16_t) logBuffer[ddd++].type;
		  if (ddd >= LOG_BUFER_SIZE) {
			  ddd = 0;
		  }
	  }

	  /* Общее количество импульсов */
	  transmitBuffer[5] = pulseCounterAvg & 0xFFFF;
	  transmitBuffer[6] = ((uint32_t) pulseCounterAvg >> 16 ) & 0xFFFF;

	  /* Импульсы за последнюю секунду */
	  transmitBuffer[7] = CPS & 0xFFFF;
	  transmitBuffer[8] = (CPS >> 16) & 0xFFFF;

	  /* Общее время в секундах от последнего старта */
	  transmitBuffer[9] =  currentTimeAvg & 0xFFFF;
	  transmitBuffer[10] = (currentTimeAvg >> 16) & 0xFFFF;

	  /* Среднее количество импульсов от последнего старта */
	  AvgCPS.Float = (float) pulseCounterAvg / (float) currentTimeAvg;
	  transmitBuffer[11] = AvgCPS.Uint[0];
	  transmitBuffer[12] = AvgCPS.Uint[1];

	 /*
	  *	Расчет температуры
	  *	 TS_CAL1 (30 °C  VREF+ = 3.0 V) 0x0BF90710 - 0x0BF90711
	  *	 TS_CAL2 (130 °C VREF+ = 3.0 V) 0x0BF90742 - 0x0BF90743
	  *
	  *	 T(°C) = (TS_CAL2_TEMP – TS_CAL1_TEMP) / (TS_CAL2 – TS_CAL1) * ( TS_DATA – TS_CAL1 ) + TS_CAL1_TEMP
	  *
	  *	 T(°C) = (130 - 30) / (*(__IO uint32_t*) 0x0BF9 0742 - *(__IO uint32_t*) 0x0BF9 0710) * (TS_DATA - *(__IO uint32_t*) 0x0BF9 0710) + 30
	  */
	  Temperature.Float = TK1 * (float) currTemperature + TK2;
	  //Temperature.Float = (float) currTemperature;
	  Voltage.Float = currVoltage * ADC_VREF_COEF;
	  //Voltage.Float = 4.2;

	  /* Сохранение температуры в буфер для передачи */
	  transmitBuffer[13] = Temperature.Uint[0];
	  transmitBuffer[14] = Temperature.Uint[1];

	  /* Напряжение батареи */
	  transmitBuffer[15] = Voltage.Uint[0];
	  transmitBuffer[16] = Voltage.Uint[1];

	  /* Коэффициенты преобразования канала в энергию для 1024 каналов*/
	  transmitBuffer[17] = enCoefA1024.Uint16[0];
	  transmitBuffer[18] = enCoefA1024.Uint16[1];
	  transmitBuffer[19] = enCoefB1024.Uint16[0];
	  transmitBuffer[20] = enCoefB1024.Uint16[1];
	  transmitBuffer[21] = enCoefC1024.Uint16[0];
	  transmitBuffer[22] = enCoefC1024.Uint16[1];

	  /* Коэффициент пересчета uRh/csp */
	  transmitBuffer[23] = calcCoeff.Uint16[0];
	  transmitBuffer[24] = calcCoeff.Uint16[1];

	  /* Уставка для источника HV */
	  transmitBuffer[25] = HVoltage;

	  /* Уставка порога компаратора */
	  transmitBuffer[26] = comparatorLevel;

	  /* Значения порогов предупреждений */
	  transmitBuffer[27] = level1;
	  transmitBuffer[28] = level2;
	  transmitBuffer[29] = level3;

	  /* Битовый регистр конфигурации */
	  transmitBuffer[30] = LEDEnable | (SoundEnable << 1) | (levelSound1 << 2) | (levelSound2 << 3) | (levelSound3 << 4) | (levelVibro1 << 5) | (levelVibro2 << 6) | (levelVibro3 << 7) | (autoStartSpecrometr << 8);
	  /* Коэффициенты преобразования канала в энергию для 2048 каналов*/
	  transmitBuffer[31] = enCoefA2048.Uint16[0];
	  transmitBuffer[32] = enCoefA2048.Uint16[1];
	  transmitBuffer[33] = enCoefB2048.Uint16[0];
	  transmitBuffer[34] = enCoefB2048.Uint16[1];
	  transmitBuffer[35] = enCoefC2048.Uint16[0];
	  transmitBuffer[36] = enCoefC2048.Uint16[1];

	  /* Коэффициенты преобразования канала в энергию для 4096 каналов*/
	  transmitBuffer[37] = enCoefA4096.Uint16[0];
	  transmitBuffer[38] = enCoefA4096.Uint16[1];
	  transmitBuffer[39] = enCoefB4096.Uint16[0];
	  transmitBuffer[40] = enCoefB4096.Uint16[1];
	  transmitBuffer[41] = enCoefC4096.Uint16[0];
	  transmitBuffer[42] = enCoefC4096.Uint16[1];

	  /* Время работы спектрометра */
	  transmitBuffer[43] = spectrometerTime & 0xFFFF;
	  transmitBuffer[44] = (spectrometerTime >> 16) & 0xFFFF;
	  /* Количество импульсов спектрометра */
	  transmitBuffer[45] = spectrometerPulse & 0xFFFF;
	  transmitBuffer[46] = (spectrometerPulse >> 16) & 0xFFFF;
	  /* Точность дозиметра, количество импульсов для усреднения. */
	  transmitBuffer[47] = dozimetrAquracy;
	  /* Разрядность канала и время выборки АЦП*/
	  //currentSamplingTime = 3;
	  if (overloadFlag) {		// Флаг перегрузки
		  transmitBuffer[48] = ((uint16_t) bitsOfChannal & 0xFF) | (((uint16_t) currentSamplingTime & 0x7) << 8)   | 0b0000100000000000;
	  } else {
		  transmitBuffer[48] = (((uint16_t) bitsOfChannal & 0xFF) | (((uint16_t) currentSamplingTime & 0x7) << 8)) & 0b1111011111111111;
	  }


	  uint16_t tmpCS = 0;			/* Очистим контрольнуюю сумму */
	  transmitBuffer[idxCS] = 0;
	  kkk = 0;
	  //uint8_t delayInterval = 0;
	  for (int iii = 0; iii < countMTU; iii++) {
		  /* Передача возможна только при подключеном клиенте */
		  if ( ! connectFlag) {
			  break;
		  }
		  for (int jjj = 0; jjj < MTUSizeValue; jjj++) {
			  uint16_t dataSpectr = transmitBuffer[kkk++];
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

		  while (!sendData(tmpBTBuffer)) {
			  MX_APPE_Process();
		  }
		  //sendData(tmpBTBuffer);
		  //MX_APPE_Process();
		  /*
		   * TODO -- требуется задержка в передаче, иначе не все пакеты принимаются
		   */
		  //if (delayInterval++ > DELAY_INTERVAL) {
			//  delayInterval = 0;
			 // HAL_Delay(SEND_DELAY);
		  //}
			#ifdef DEBUG_USER
			bzero((char *) uartBuffer, sizeof(uartBuffer));
			sprintf(uartBuffer, "MTU: %d\n\r", iii);
			HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
			#endif
	  }
		#ifdef DEBUG_USER
		bzero((char *) uartBuffer, sizeof(uartBuffer));
		sprintf(uartBuffer, "CS: %d, MTU: %d\n\r", tmpCS, MTUSizeValue);
		HAL_UART_Transmit(&huart2, (uint8_t *) uartBuffer, strlen(uartBuffer), 100);
		#endif
	}

    /*if (interval1 < intervalNow) {
    	interval1 = intervalNow + INTERVAL1;
    	//HAL_GPIO_TogglePin(LED_GPIO_Port, LED_Pin);
    	//NotifyAct(LED_NOTIFY, 0);
    	//NotifyAct(SOUND_NOTIFY, 2);
		if (connectFlag) {
			//NotifyAct(LED_NOTIFY, 0);

		}
    }*/
    /* Измерение напряжения батареи и температуры МК */
    //if (interval4 < intervalNow) {
    //	interval4 = intervalNow + INTERVAL4;
    //	if (! flagTemperatureMess) {
    		/* Включим ADC для трех каналов */
    		//MODIFY_REG(hadc4.Instance->CHSELR, ADC_CHSELR_SQ_ALL, ((ADC_CHSELR_SQ2 | ADC_CHSELR_SQ3 | ADC_CHSELR_SQ4 | ADC_CHSELR_SQ5 | ADC_CHSELR_SQ6 | ADC_CHSELR_SQ7 | ADC_CHSELR_SQ8) << (((3UL - 1UL) * ADC_REGULAR_RANK_2) & 0x1FUL)) | (hadc4.ADCGroupRegularSequencerRanks));
	//		flagTemperatureMess = true;						// Для единичного измерения
    //	}
    //}
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
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.APB7CLKDivider = RCC_HCLK_DIV1;
  RCC_ClkInitStruct.AHB5_PLL1_CLKDivider = RCC_SYSCLK_PLL1_DIV1;
  RCC_ClkInitStruct.AHB5_HSEHSI_CLKDivider = RCC_SYSCLK_HSEHSI_DIV1;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_1) != HAL_OK)
  {
    Error_Handler();
  }

   /* Select SysTick source clock */
  HAL_SYSTICK_CLKSourceConfig(SYSTICK_CLKSOURCE_LSE);

   /* Re-Initialize Tick with new clock source */
  if (HAL_InitTick(TICK_INT_PRIORITY) != HAL_OK)
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
  * @brief Power Configuration
  * @retval None
  */
static void SystemPower_Config(void)
{
  /* WKUP_IRQn interrupt configuration */
  HAL_NVIC_SetPriority(WKUP_IRQn, 5, 0);
  HAL_NVIC_EnableIRQ(WKUP_IRQn);
/* USER CODE BEGIN PWR */
/* USER CODE END PWR */
}

/**
  * @brief ADC4 Initialization Function
  * @param None
  * @retval None
  */
void MX_ADC4_Init(void)
{

  /* USER CODE BEGIN ADC4_Init 0 */

  /* USER CODE END ADC4_Init 0 */

  ADC_ChannelConfTypeDef sConfig = {0};

  /* USER CODE BEGIN ADC4_Init 1 */
  if (dataType == onlyDozimeter) {
	  /* ACD конфигурация для дозиметра */
  /* USER CODE END ADC4_Init 1 */

  /** Common config
  */
  hadc4.Instance = ADC4;
  hadc4.Init.ClockPrescaler = ADC_CLOCK_ASYNC_DIV1;
  hadc4.Init.Resolution = ADC_RESOLUTION_12B;
  hadc4.Init.DataAlign = ADC_DATAALIGN_RIGHT;
  hadc4.Init.ScanConvMode = ADC_SCAN_DISABLE;
  hadc4.Init.EOCSelection = ADC_EOC_SINGLE_CONV;
  hadc4.Init.LowPowerAutoPowerOff = DISABLE;
  hadc4.Init.LowPowerAutonomousDPD = ADC_LP_AUTONOMOUS_DPD_DISABLE;
  hadc4.Init.LowPowerAutoWait = DISABLE;
  hadc4.Init.ContinuousConvMode = DISABLE;
  hadc4.Init.NbrOfConversion = 1;
  hadc4.Init.ExternalTrigConv = ADC_SOFTWARE_START;
  hadc4.Init.ExternalTrigConvEdge = ADC_EXTERNALTRIGCONVEDGE_NONE;
  hadc4.Init.DMAContinuousRequests = ENABLE;
  hadc4.Init.TriggerFrequencyMode = ADC_TRIGGER_FREQ_HIGH;
  hadc4.Init.Overrun = ADC_OVR_DATA_PRESERVED;
  hadc4.Init.SamplingTimeCommon1 = ADC_SAMPLETIME_1CYCLE_5;
  hadc4.Init.SamplingTimeCommon2 = ADC_SAMPLETIME_814CYCLES_5;
  hadc4.Init.OversamplingMode = DISABLE;
  if (HAL_ADC_Init(&hadc4) != HAL_OK)
  {
    Error_Handler();
  }

  /** Configure Regular Channel
  */
  sConfig.Channel = ADC_CHANNEL_TEMPSENSOR;
  sConfig.Rank = ADC_REGULAR_RANK_1;
  sConfig.SamplingTime = ADC_SAMPLINGTIME_COMMON_2;
  if (HAL_ADC_ConfigChannel(&hadc4, &sConfig) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN ADC4_Init 2 */
  /* ADC конфигурация для спектрометра */
  } else {
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
	  hadc4.Init.NbrOfConversion = 1;
	  hadc4.Init.DiscontinuousConvMode = DISABLE;
	  hadc4.Init.ExternalTrigConv = ADC_EXTERNALTRIG_EXT_IT15;
	  hadc4.Init.ExternalTrigConvEdge = ADC_EXTERNALTRIGCONVEDGE_RISING;
	  hadc4.Init.DMAContinuousRequests = ENABLE;
	  hadc4.Init.TriggerFrequencyMode = ADC_TRIGGER_FREQ_HIGH;
	  //hadc4.Init.TriggerFrequencyMode = ADC_TRIGGER_FREQ_LOW;
	  //hadc4.Init.Overrun = ADC_OVR_DATA_PRESERVED;
	  hadc4.Init.Overrun = ADC_OVR_DATA_OVERWRITTEN;
	  hadc4.Init.SamplingTimeCommon1 = samplingTm(currentSamplingTime);
	  //hadc4.Init.SamplingTimeCommon1 = ADC_SAMPLETIME_1CYCLE_5;		// для LaBr3:Ce
	  //hadc4.Init.SamplingTimeCommon1 = ADC_SAMPLETIME_3CYCLES_5;		// для Sensl FC/FJ60035 + NaI:Tl
	  //hadc4.Init.SamplingTimeCommon1 = ADC_SAMPLETIME_7CYCLES_5;
	  //hadc4.Init.SamplingTimeCommon1 = ADC_SAMPLETIME_12CYCLES_5;
	  //hadc4.Init.SamplingTimeCommon1 = ADC_SAMPLETIME_79CYCLES_5;		// для MacroPixel SC-14x25c-SiPM-T
	  hadc4.Init.SamplingTimeCommon2 = ADC_SAMPLETIME_814CYCLES_5;
	  hadc4.Init.OversamplingMode = DISABLE;
	  if (HAL_ADC_Init(&hadc4) != HAL_OK)
	  {
	    Error_Handler();
	  }
	  /* Configure Regular Channel */
	  sConfig.Channel = ADC_CHANNEL_9;
	  sConfig.Rank = ADC_REGULAR_RANK_1;
	  sConfig.SamplingTime = ADC_SAMPLINGTIME_COMMON_1;
	  if (HAL_ADC_ConfigChannel(&hadc4, &sConfig) != HAL_OK)
	  {
	    Error_Handler();
	  }
  }
  /* USER CODE END ADC4_Init 2 */

}

/**
  * @brief CRC Initialization Function
  * @param None
  * @retval None
  */
void MX_CRC_Init(void)
{

  /* USER CODE BEGIN CRC_Init 0 */

  /* USER CODE END CRC_Init 0 */

  /* USER CODE BEGIN CRC_Init 1 */

  /* USER CODE END CRC_Init 1 */
  hcrc.Instance = CRC;
  hcrc.Init.DefaultPolynomialUse = DEFAULT_POLYNOMIAL_DISABLE;
  hcrc.Init.DefaultInitValueUse = DEFAULT_INIT_VALUE_ENABLE;
  hcrc.Init.GeneratingPolynomial = 7;
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
void MX_GPDMA1_Init(void)
{

  /* USER CODE BEGIN GPDMA1_Init 0 */

  /* USER CODE END GPDMA1_Init 0 */

  /* Peripheral clock enable */
  LL_AHB1_GRP1_EnableClock(LL_AHB1_GRP1_PERIPH_GPDMA1);

  /* GPDMA1 interrupt Init */
  NVIC_SetPriority(GPDMA1_Channel0_IRQn, NVIC_EncodePriority(NVIC_GetPriorityGrouping(),4, 0));
  NVIC_EnableIRQ(GPDMA1_Channel0_IRQn);

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
void MX_ICACHE_Init(void)
{

  /* USER CODE BEGIN ICACHE_Init 0 */

  /* USER CODE END ICACHE_Init 0 */

  /* USER CODE BEGIN ICACHE_Init 1 */

  /* USER CODE END ICACHE_Init 1 */

  /** Full retention for ICACHE in stop mode
  */
  LL_PWR_SetICacheRAMStopRetention(LL_PWR_ICACHERAM_STOP_FULL_RETENTION);

  /** Enable instruction cache in 1-way (direct mapped cache)
  */
  LL_ICACHE_SetMode(LL_ICACHE_1WAY);
  LL_ICACHE_Enable();
  /* USER CODE BEGIN ICACHE_Init 2 */

  /* USER CODE END ICACHE_Init 2 */

}

/**
  * @brief LPTIM2 Initialization Function
  * @param None
  * @retval None
  */
void MX_LPTIM2_Init(void)
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
  hlptim2.Init.Period = 6;
  hlptim2.Init.UpdateMode = LPTIM_UPDATE_IMMEDIATE;
  hlptim2.Init.CounterSource = LPTIM_COUNTERSOURCE_INTERNAL;
  hlptim2.Init.Input1Source = LPTIM_INPUT1SOURCE_GPIO;
  hlptim2.Init.Input2Source = LPTIM_INPUT2SOURCE_GPIO;
  hlptim2.Init.RepetitionCounter = 0;
  if (HAL_LPTIM_Init(&hlptim2) != HAL_OK)
  {
    Error_Handler();
  }
  sConfig1.Pulse = 3;
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
void MX_RAMCFG_Init(void)
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
void MX_RNG_Init(void)
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
  RTC_AlarmTypeDef sAlarm = {0};

  /* USER CODE BEGIN RTC_Init 1 */

  /* USER CODE END RTC_Init 1 */

  /** Initialize RTC Only
  */
  hrtc.Instance = RTC;
  hrtc.Init.AsynchPrediv = 31;
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

  /* USER CODE BEGIN Check_RTC_BKUP */

  /* USER CODE END Check_RTC_BKUP */

  /** Initialize RTC and set the Time and Date
  */
  if (HAL_RTCEx_SetSSRU_IT(&hrtc) != HAL_OK)
  {
    Error_Handler();
  }

  /** Enable the Alarm A
  */
  sAlarm.BinaryAutoClr = RTC_ALARMSUBSECONDBIN_AUTOCLR_NO;
  sAlarm.AlarmTime.SubSeconds = 0x0;
  sAlarm.AlarmMask = RTC_ALARMMASK_NONE;
  sAlarm.AlarmSubSecondMask = RTC_ALARMSUBSECONDBINMASK_NONE;
  sAlarm.Alarm = RTC_ALARM_A;
  if (HAL_RTC_SetAlarm_IT(&hrtc, &sAlarm, RTC_FORMAT_BCD) != HAL_OK)
  {
    Error_Handler();
  }
  /* USER CODE BEGIN RTC_Init 2 */

  /* USER CODE END RTC_Init 2 */

}

/**
  * @brief GPIO Initialization Function
  * @param None
  * @retval None
  */
void MX_GPIO_Init(void)
{
  GPIO_InitTypeDef GPIO_InitStruct = {0};
/* USER CODE BEGIN MX_GPIO_Init_1 */
/* USER CODE END MX_GPIO_Init_1 */

  /* GPIO Ports Clock Enable */
  __HAL_RCC_GPIOB_CLK_ENABLE();
  __HAL_RCC_GPIOA_CLK_ENABLE();
  __HAL_RCC_GPIOC_CLK_ENABLE();

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOB, A_DATA_Pin|NC_Pin|NCB8_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pin Output Level */
  HAL_GPIO_WritePin(GPIOA, A_SCK_Pin|A_CS_Pin|VIBRO_Pin|LED_Pin, GPIO_PIN_RESET);

  /*Configure GPIO pins : A_DATA_Pin NC_Pin NCB8_Pin */
  GPIO_InitStruct.Pin = A_DATA_Pin|NC_Pin|NCB8_Pin;
  GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
  GPIO_InitStruct.Pull = GPIO_NOPULL;
  GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
  HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

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
  HAL_NVIC_SetPriority(EXTI15_IRQn, 6, 0);
  HAL_NVIC_EnableIRQ(EXTI15_IRQn);

/* USER CODE BEGIN MX_GPIO_Init_2 */
  /* Начальная устанока уровней для компаратора и высокого */
  setLevelOnPort(CHANNEL_A, 0x3FF);
  setLevelOnPort(CHANNEL_B, 0x3FF);
/* USER CODE END MX_GPIO_Init_2 */
}

/* USER CODE BEGIN 4 */
void updateMesurmentCb(void *arg) {
	UTIL_SEQ_SetTask(1<<CFG_TASK_MEASURE_REQ_ID, CFG_SEQ_PRIO_0);
}

/*
 *	Вызывается раз в секунду по таймеру
 */
void updateMesurment(void) {
	if (firstInital) {
		  pulseCounter = 0;
		  pulseCounterSecond = 0;
		  currentTime = 0;
		  firstInital = false;
		  for (int ii = 0; ii < SIZE_DOZIMETR_BUFER; ii++) {
			  dozimetrBuffer[ii] = 0;
		  }
	}
		//if (! connectFlag) {
		//	UTIL_LPM_SetStopMode(1U << CFG_LPM_LOG, UTIL_LPM_ENABLE);
		//}
		//NotifyAct(SOUND_NOTIFY | VIBRO_NOTIFY, 2);
		//HAL_GPIO_TogglePin(LED_GPIO_Port, LED_Pin);
		//HAL_GPIO_TogglePin(VIBRO_GPIO_Port, VIBRO_Pin);
		//NotifyAct(LED_NOTIFY, 0);
		intervalNow++;
		aquracyInterval++;
		currentTimeAvg = currentTime++;
		pulseCounterAvg = pulseCounter;
		CPS = pulseCounterSecond;
		if (dataType > onlyDozimeter) {
			spectrometerTime++;
		}
		/* Массив для гистограммы уровней */
		/*
		if (indexDozimetrBufer >= SIZE_DOZIMETR_BUFER) {
			ndexDozimetrBufer = 0;
		}
		dozimetrBuffer[indexDozimetrBufer++] = pulseCounterSecond;
		*/
		/* Проверка условия насыщения */
		if ((CPS == 0) && ((Sync_GPIO_Port->IDR & Sync_Pin) != 0x00U)) {
			/* Сюда попадаем если SiPM в насыщении, на компараторе высокий уровень */
			logUpdate(overload);
			overloadFlag = true;
			/* Данные для рекламного пакета - переполнение */
			APP_BLE_Update_Manufacturer_Data(0xFFFFFFFF);
		} else {
			overloadFlag = false;
			/* Данные для рекламного пакета - нормальный CPS*/
			APP_BLE_Update_Manufacturer_Data(CPS);
		}

		pulseCounterSecond = 0;
		/*
		 * Анализ CPS для управления порогами срабатывания сигнализации
		 */
		tmp_level = 0;
		uint8_t tmpNotify = 0;
		if ((level1 > 0) && (CPS > level1_cps)) {
			tmp_level = 1;
			if (levelSound1 || levelVibro1) {
				if (levelSound1) {					// Звук на перврм уровне разрешен
					tmpNotify = SOUND_NOTIFY;
				}

				if (levelVibro1) {					// Вибро на первом уровне разрешено
					tmpNotify |= VIBRO_NOTIFY;
				}
			}
		}
		if ((level2 > 0) && (CPS > level2_cps)) {
			tmp_level = 2;
			tmpNotify = 0;
			if (levelSound2) {					// Звук на втором уровне разрешен
				tmpNotify = SOUND_NOTIFY;
			}

			if (levelVibro2) {					// Вибро на втором уровне разрешено
				tmpNotify |= VIBRO_NOTIFY;
			}
		}
		if ((level3 > 0) && (CPS > level3_cps)) {
			tmp_level = 3;
			tmpNotify = 0;
			if (levelSound3) {					// Звук на третьем уровне разрешен
				tmpNotify = SOUND_NOTIFY;
			}

			if (levelVibro3) {					// Вибро на третьем уровне разрешено
				tmpNotify |= VIBRO_NOTIFY;
			}
		}
		/* Тревога если превышение. */
		if (tmp_level > 0) {
			history_active = true;
			if (tmpNotify > 0) {
				NotifyAct(tmpNotify, tmp_level);
			}
		} else {
			history_active = false;
		}

		if (currentLevel != tmp_level) {
			currentLevel = tmp_level;
			/*
			 *	act
			 *	2 - превышение уровня 1
			 *	3 - превышение уровня 2
			 *	4 - превышение уровня 3
			 *	5 - нормальный уровень
			 */
			if (tmp_level == 0) {
				logUpdate(level0Log);					// Нормальный уровень
			} else {
				logUpdate((logTypes_t) tmp_level + 1);		// Превышение уровня
			}
		}
		/* Измерение напряжения батареи и температуры МК */
		if (interval4 <= intervalNow) {
			interval4 = intervalNow + INTERVAL4;
			TVMeasure();
		}
}

uint32_t ADC_Switch_Channel(uint32_t channel) {
	uint32_t tmp_level;
    ADC_ChannelConfTypeDef sConfig = {0};
    sConfig.Channel = channel;
    sConfig.Rank = ADC_REGULAR_RANK_1; 					// Всегда используем Rank = 1 для ручного режима
    sConfig.SamplingTime = ADC_SAMPLINGTIME_COMMON_2;
    HAL_ADC_ConfigChannel(&hadc4, &sConfig);
	HAL_ADC_Start(&hadc4);
	HAL_ADC_PollForConversion(&hadc4, HAL_MAX_DELAY);
	tmp_level = HAL_ADC_GetValue(&hadc4) & 0xFFF;
	HAL_ADC_Stop(&hadc4);
	return tmp_level;
}

void TVMeasure(void) {
	if (dataType == onlyDozimeter ) {
		//UTIL_LPM_SetStopMode(1U << CFG_LPM_LOG, UTIL_LPM_DISABLE);
		MX_ADC4_Init();
		currTemperature = ADC_Switch_Channel(ADC_CHANNEL_TEMPSENSOR);	// Канал для измерения температуры
		currVoltage = ADC_Switch_Channel(ADC_CHANNEL_7);				// Канал для измерения напряжения.
		HAL_ADC_DeInit(&hadc4);											// Для снижения потребления
	} else {
		/* Измерение температуры и напряжения в режиме спектрометра */
		sendDataTypes_t tmp_data_type = dataType;								// Сохраним текущий режим.
		HAL_ADC_Stop_DMA(&hadc4);										// Отключим набор спектра
		HAL_ADC_DeInit(&hadc4);
		dataType = onlyDozimeter;
		MX_ADC4_Init();													// Инициализируем ADC в режиме для программного запуска.
		currTemperature = ADC_Switch_Channel(ADC_CHANNEL_TEMPSENSOR); 	// Канал для измерения температуры
		currVoltage = ADC_Switch_Channel(ADC_CHANNEL_7);				// Канал для измерения напряжения.

		HAL_ADC_DeInit(&hadc4);											// Для снижения потребления
		//HAL_DMA_DeInit(&handle_GPDMA1_Channel0);
		dataType = tmp_data_type;										// Возвращаем исходный режим спектрометра
		MX_ADC4_Init();
		//HAL_ADCEx_Calibration_Start(&hadc4);
		//LL_ADC_StartCalibration(ADC4);								// Запуск калибровки.
		//while (LL_ADC_IsCalibrationOnGoing(ADC4) != 0);				// Ожидаем конца калибровки.
		//MX_GPDMA1_Init();
		HAL_ADC_Start_DMA(&hadc4, TVLevel, 1);
		//hadc4.DMA_Handle->Instance->CCR &= ~DMA_IT_HT;				// Отключение прерывания по промежуточному значению.
	}
	UTIL_LPM_SetStopMode(1U << CFG_LPM_LOG, UTIL_LPM_ENABLE);
}

/*
void HAL_ADC_ConvCpltCallback(ADC_HandleTypeDef* hadc) {
  if(hadc->Instance == ADC4) {
	//if (flagTemperatureMess) {
		//flagTemperatureMess = false;						// Сбросим флаг однократного выполнения.
		currVoltage = (uint16_t) TVLevel[1] & 0xFFF;		// Сохраним напряжение
		currTemperature = (uint16_t) TVLevel[0] & 0xFFF;	// Сохраним температуру
	//}
  }
}
*/

void updateLedCb (void *arg) {
	UTIL_SEQ_SetTask(1<<CFG_TASK_LED_REQ_ID, CFG_SEQ_PRIO_2);
}

void updateVibroCb(void *arg) {
	UTIL_SEQ_SetTask(1<<CFG_TASK_VIBRO_REQ_ID, CFG_SEQ_PRIO_2);
}

void updateVibroOffCb(void *arg) {
	UTIL_SEQ_SetTask(1<<CFG_TASK_VIBROOFF_REQ_ID, CFG_SEQ_PRIO_2);
}

/* Таймер для задания интервала для вибро и звука*/
void vibroActivate(void) {
	if (vibroFlag) {
		//HAL_GPIO_WritePin(VIBRO_GPIO_Port, VIBRO_Pin, GPIO_PIN_SET);
		VIBRO_GPIO_Port->BSRR = (uint32_t) VIBRO_Pin;		// Включение вибро.
	}
	if (soundFlag) {
		HAL_LPTIM_PWM_Start(&hlptim2, LPTIM_CHANNEL_2);		// Включение звука.
	}
	if (vibroFlag || soundFlag) {
		UTIL_TIMER_Start(&(timerVibroOff));					// Таймер для отключения (vibroActivateOff()) звука/вибро.
		if (--repetionCount > 0) {
			UTIL_TIMER_Start(&(timerVibro));				// Повторный вызов (vibroActivate()) для включения звука/вибро.
		} else {
			vibroFlag = false;
			soundFlag = false;
		}
	}
}

/* Таймер для отключения LED*/
void ledActivate(void) {
	//HAL_GPIO_WritePin(LED_GPIO_Port, LED_Pin, GPIO_PIN_RESET);
	LED_GPIO_Port->BRR = (uint32_t) LED_Pin;
}

/* Таймер для отключения вибро */
void vibroActivateOff(void) {
	HAL_LPTIM_PWM_Stop(&hlptim2, LPTIM_CHANNEL_2);		// Отключение звука.
	//HAL_GPIO_WritePin(VIBRO_GPIO_Port, VIBRO_Pin, GPIO_PIN_RESET);
	VIBRO_GPIO_Port->BRR = (uint32_t) VIBRO_Pin;		// Отключение вибро.
}

/* Обработка прерываний по приходу импульсов */
void HAL_GPIO_EXTI_Rising_Callback(uint16_t GPIO_Pin) {
	  pulseCounter++;					// Общее количество импульсов с начала измерения
	  pulseCounterSecond++;				// Количество импульсов за последнюю секунду
		/* Оповещение об импульсе */
	  if (LEDEnable) {
		  NotifyAct(LED_NOTIFY, 0);
	  }
	  /* Расчет с учетом точности */
	  if (edgeCounter++ >= dozimetrAquracy) {
		  if (aquracyInterval > 0) {
			  if (indexDozimetrBufer >= SIZE_DOZIMETR_BUFER) {
				indexDozimetrBufer = 0;
			  }
			  dozimetrBuffer[indexDozimetrBufer++] = edgeCounter / aquracyInterval;
			  //dozimetrBuffer[indexDozimetrBufer++] = 1;
			  aquracyInterval = 0;
			  edgeCounter = 0;
		  }
	  }
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
