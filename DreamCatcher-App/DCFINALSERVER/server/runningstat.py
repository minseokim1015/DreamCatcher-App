import numpy as np
import math


class Stddev:
    def __init__(self):
        self.mean = None
        self.var = None
        self.cnt = 0

    def append(self, x):
        self.cnt += 1
        if self.mean is None:
            self.mean = x
            self.var = 0.
        else:
            new_mean = (1 / self.cnt) * (x + (self.cnt - 1) * self.mean)
            self.var = (((self.cnt - 2) * self.var + (self.mean - x) ** 2) / (self.cnt - 1)) + (new_mean - self.mean)
            self.mean = new_mean
        return np.sqrt(self.var)


class RunningStats:
    def __init__(self):
        self.n = 0
        self.old_m = 0
        self.new_m = 0
        self.old_s = 0
        self.new_s = 0

    def clear(self):
        self.n = 0

    def push(self, x):
        self.n += 1

        if self.n == 1:
            self.old_m = self.new_m = x
            self.old_s = 0
        else:
            self.new_m = self.old_m + (x - self.old_m) / self.n
            self.new_s = self.old_s + (x - self.old_m) * (x - self.new_m)

            self.old_m = self.new_m
            self.old_s = self.new_s

    def mean(self):
        return self.new_m if self.n else 0.0

    def variance(self):
        return self.new_s / (self.n - 1) if self.n > 1 else 0.0

    def standard_deviation(self):
        return math.sqrt(self.variance())
