% Параметры SiPM
%cells = 18980.0;    % Количество ячеек sensl FC60036
%tau = 95.0;         % Время восстановления sensl FC60035
%PDE = 41.0;         % Эффективность SiPM sensl FC60035
%CROSS_TALK = 0.07;  % Эфект активации соседних ячеек sensl FC60035

cells = 159984.0;    % Количество ячеек EQR15 11-6060D-S
tau = 9.0;           % Время восстановления EQR15 11-6060D-S
PDE = 45.0;          % Эффективность EQR15 11-6060D-S
CROSS_TALK = 0.1;    % Эфект активации соседних ячеек EQR15 11-6060D-S


% Параметры кристалла
EFF = 63000.0;       % Эффективность кристалла BrLa3:Ce
%EFF = 10000.0;      % Эффективность кристалла EPS100
%EFF = 38000.0       % Эффективность кристалла NaI(Tl)

POWER = 3.0;         % Энергия гамма-кванта в МЭв
interval = 20.0;     % Интервал между импульсами.

numbersFotons = EFF * POWER;
% Расчет количества фотонов с учетом PDE, световыхода кристалла и энергии
% частици
totalPulse = numbersFotons * PDE / 100 / (1 - CROSS_TALK);


% Количество активных ячеек после первого импульса.
fotonsPerCell = totalPulse / cells;
partActive = 1 - exp(-fotonsPerCell);
regPulses = partActive * cells;

% Количество доступных ячеек, после первого импульса.
avalAfter = 1 - exp(- interval / tau);
regPulseKoef = 1 - partActive + partActive * avalAfter;

regPulses2 = regPulseKoef * cells;
effPDE = PDE * regPulseKoef;
fotonsSecond = effPDE * numbersFotons / 100;
avgNoCell = fotonsSecond / regPulses2;
partActiveSecond = 1 - exp(-avgNoCell);
actCellSecond = regPulses2 * partActiveSecond;

fprintf("Всего фотонов: %d\n", numbersFotons);
fprintf("Всего фотонов с учетом PDE и Cross_talk: %.0f\n", totalPulse);
fprintf("Фотонов на ячейку: %.6f\n", fotonsPerCell);
fprintf("Активировано ячеек на первом импульсе: %.0f\n", regPulses);
fprintf("Доступно ячеек до второго импульса: %.0f\n", regPulses2);
fprintf("Фотонов от второго импульса с учетом PDE: %.0f\n", fotonsSecond);
fprintf("Фотонов на ячейку: %.6f\n", avgNoCell);
fprintf("Сработает ячеек при втором импульсе: %.0f\n", actCellSecond);
