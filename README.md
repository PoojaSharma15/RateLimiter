## Rate Limiter Project

This project implements a Rate Limiter using two different algorithms:

1. Sliding Window Log
2. Sliding Window Counter

## Overview

A Rate Limiter is a mechanism used to control the rate of requests sent or received by a service. It ensures that the number of requests does not exceed a specified limit within a defined time window. This helps to protect services from being overwhelmed by too many requests in a short period of time.

## Algorithms

### 1. Sliding Window Log

The Sliding Window Log algorithm maintains a log of timestamps for each request. It allows requests as long as the number of requests within the current window does not exceed the defined limit. This method provides high accuracy but can consume more memory due to storing individual request timestamps.

### 2. Sliding Window Counter

The Sliding Window Counter algorithm divides the time window into smaller buckets and counts the number of requests in each bucket. The total count of requests in the relevant buckets is used to determine if the request limit has been exceeded. This method is more memory-efficient but may be less precise.
