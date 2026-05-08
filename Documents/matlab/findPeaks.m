channels = 1:1024;
spectrum = importdata('fon.dat');

% Вызов функции поиска пиков
peak_positions = find_peaks_spectrum(spectrum);