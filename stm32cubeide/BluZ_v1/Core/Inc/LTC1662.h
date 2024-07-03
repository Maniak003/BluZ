/*
 * LTC1662.h
 *
 *  Created on: Jun 30, 2024
 *      Author: ed
 */
#include "main.h"

#ifndef INC_LTC1662_H_
#define INC_LTC1662_H_

extern uint16_t currentLevelPortA;
extern uint16_t currentLevelPortB;

void setLevelOnPort(uint8_t port, uint16_t level);

#endif /* INC_LTC1662_H_ */
