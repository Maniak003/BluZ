% 1. Постройте матрицу отклика без регуляризации
figure;
imagesc(energies_keV, bin_centers, R');
set(gca, 'YDir', 'normal'); colorbar;
xlabel('Energy (keV)'); ylabel('Bin (MeV)');
title('Response Matrix R (no regularization)');

% 2. Посмотрите, есть ли пики в самих столбцах R
idx_test = [find(energies_keV==1500), find(energies_keV==2000), find(energies_keV==2614)];
figure;
for i = 1:3
    subplot(3,1,i);
    plot(bin_centers, R(:,idx_test(i))*1e6, 'b-');  % нормировка на 10^6 событий
    xlim([0 3]); grid on;
    title(sprintf('Response at %.0f keV', energies_keV(idx_test(i))));
end

% 3
% Проверка покрытия калибровочных источников
calib_sources = [59.5, 122, 662, 1173, 1332, 2614];  % кэВ
for E = calib_sources
    [~, idx] = min(abs(energies_keV - E));
    fprintf('%.1f keV: ближайшая точка симуляции = %.1f keV (ошибка %.1f%%)\n', ...
        E, energies_keV(idx), 100*abs(energies_keV(idx)-E)/E);
end