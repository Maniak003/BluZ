%
%   coder
%
function y = sma(data, window_size)
    kernel = ones(1, window_size) / window_size;
    y = conv(data, kernel, 'same');
end
