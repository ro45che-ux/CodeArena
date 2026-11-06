-- CodeArena: additional practice problems.
--
-- Safe to run more than once: a problem is only inserted if no problem with the same title exists,
-- and its test cases are only inserted if that problem has none yet. Nothing is ever deleted.
--
-- Works on a database whose tables were created by the backend (Hibernate) or by db/init.sql:
--   psql "<DATABASE_URL>" -f db/seed_problems.sql
--
-- Text values are plain SQL strings (backslashes are literal, newlines are real newlines).

-- Palindrome Number (EASY)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Palindrome Number',
'Given an integer x, return true if x is a palindrome, and false otherwise.

An integer is a palindrome when it reads the same forward and backward. Negative numbers are never palindromes.

Input: a single integer x.
Output: true or false.',
'EASY',
'public static boolean isPalindrome(int x) {
    // Write your solution here

    return false;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int x = Integer.parseInt(sc.nextLine().trim());
    System.out.print(isPalindrome(x));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Palindrome Number');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('121', 'true', false),
('-121', 'false', false),
('10', 'false', true),
('12321', 'true', true),
('0', 'true', true),
('123456789', 'false', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Palindrome Number'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Fibonacci Number (EASY)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Fibonacci Number',
'The Fibonacci numbers form a sequence where each number is the sum of the two before it, starting from 0 and 1:
F(0) = 0, F(1) = 1, F(n) = F(n-1) + F(n-2).

Given n, return F(n). Use a loop or memoisation; a plain recursive solution is too slow for n = 50.

Input: a single integer n (0 <= n <= 50).
Output: F(n).',
'EASY',
'public static long fib(int n) {
    // Write your solution here

    return 0;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    System.out.print(fib(n));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Fibonacci Number');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('0', '0', false),
('1', '1', false),
('10', '55', false),
('20', '6765', true),
('30', '832040', true),
('50', '12586269025', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Fibonacci Number'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Valid Parentheses (EASY)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Valid Parentheses',
'Given a string s containing only the characters ( ) [ ] { }, determine whether the input string is valid.

A string is valid when every opening bracket is closed by the same type of bracket, and brackets are closed in the correct order.

Input: a single non-empty line containing the string s.
Output: true or false.',
'EASY',
'public static boolean isValid(String s) {
    // Write your solution here

    return false;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    String s = sc.nextLine().trim();
    System.out.print(isValid(s));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Valid Parentheses');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('()', 'true', false),
('()[]{}', 'true', false),
('(]', 'false', false),
('([)]', 'false', true),
('{[]}', 'true', true),
('((', 'false', true),
(')', 'false', true),
('{[()()]}[]', 'true', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Valid Parentheses'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Maximum Subarray (MEDIUM)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Maximum Subarray',
'Given an integer array nums, find the contiguous subarray (containing at least one number) which has the largest sum, and return that sum.

Input: the first line is n, the length of the array; the second line contains the n integers separated by spaces.
Output: the largest subarray sum.',
'MEDIUM',
'public static int maxSubArray(int[] nums) {
    // Write your solution here

    return 0;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    int[] nums = new int[n];
    String[] parts = sc.nextLine().trim().split("\\s+");
    for (int i = 0; i < n; i++) nums[i] = Integer.parseInt(parts[i]);
    System.out.print(maxSubArray(nums));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Maximum Subarray');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('9
-2 1 -3 4 -1 2 1 -5 4', '6', false),
('1
1', '1', false),
('5
5 4 -1 7 8', '23', false),
('3
-3 -2 -5', '-2', true),
('7
-1 -2 3 -4 5 -6 7', '7', true),
('5
8 -19 5 -4 20', '21', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Maximum Subarray'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Climbing Stairs (EASY)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Climbing Stairs',
'You are climbing a staircase with n steps. Each time you can climb either 1 or 2 steps. In how many distinct ways can you climb to the top?

Input: a single integer n (1 <= n <= 40).
Output: the number of distinct ways.',
'EASY',
'public static int climbStairs(int n) {
    // Write your solution here

    return 0;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    System.out.print(climbStairs(n));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Climbing Stairs');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('2', '2', false),
('3', '3', false),
('5', '8', false),
('1', '1', true),
('10', '89', true),
('40', '165580141', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Climbing Stairs'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Binary Search (EASY)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Binary Search',
'Given an array of integers nums sorted in ascending order (all values distinct) and an integer target, return the index of target in nums, or -1 if it is not present.

Your solution must run in O(log n) time.

Input: the first line is n, the second line contains the n sorted integers, the third line contains target.
Output: the index of target, or -1.',
'EASY',
'public static int search(int[] nums, int target) {
    // Write your solution here

    return -1;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    int[] nums = new int[n];
    String[] parts = sc.nextLine().trim().split("\\s+");
    for (int i = 0; i < n; i++) nums[i] = Integer.parseInt(parts[i]);
    int target = Integer.parseInt(sc.nextLine().trim());
    System.out.print(search(nums, target));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Binary Search');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('6
-1 0 3 5 9 12
9', '4', false),
('6
-1 0 3 5 9 12
2', '-1', false),
('1
5
5', '0', false),
('7
1 3 5 7 9 11 13
13', '6', true),
('7
1 3 5 7 9 11 13
1', '0', true),
('2
2 4
3', '-1', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Binary Search'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Missing Number (EASY)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Missing Number',
'Given an array nums containing n distinct numbers taken from the range 0 to n, return the one number in that range that is missing from the array.

Input: the first line is n, the second line contains the n integers separated by spaces.
Output: the missing number.',
'EASY',
'public static int missingNumber(int[] nums) {
    // Write your solution here

    return 0;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    int[] nums = new int[n];
    String[] parts = sc.nextLine().trim().split("\\s+");
    for (int i = 0; i < n; i++) nums[i] = Integer.parseInt(parts[i]);
    System.out.print(missingNumber(nums));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Missing Number');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('3
3 0 1', '2', false),
('2
0 1', '2', false),
('9
9 6 4 2 3 5 7 0 1', '8', false),
('1
0', '1', true),
('1
1', '0', true),
('4
4 2 0 1', '3', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Missing Number'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Single Number (EASY)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Single Number',
'Given a non-empty array of integers nums, every element appears exactly twice except for one element which appears only once. Find that single element.

Try to solve it in linear time using constant extra space.

Input: the first line is n, the second line contains the n integers separated by spaces.
Output: the element that appears once.',
'EASY',
'public static int singleNumber(int[] nums) {
    // Write your solution here

    return 0;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    int[] nums = new int[n];
    String[] parts = sc.nextLine().trim().split("\\s+");
    for (int i = 0; i < n; i++) nums[i] = Integer.parseInt(parts[i]);
    System.out.print(singleNumber(nums));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Single Number');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('3
2 2 1', '1', false),
('5
4 1 2 1 2', '4', false),
('1
1', '1', false),
('3
-1 7 -1', '7', true),
('5
10 20 30 20 10', '30', true),
('7
5 3 5 9 3 9 8', '8', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Single Number'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Best Time to Buy and Sell Stock (EASY)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Best Time to Buy and Sell Stock',
'You are given an array prices where prices[i] is the price of a stock on day i. You want to maximise your profit by choosing one day to buy and a later day to sell.

Return the maximum profit you can achieve. If no profit is possible, return 0.

Input: the first line is n, the second line contains the n prices separated by spaces.
Output: the maximum profit.',
'EASY',
'public static int maxProfit(int[] prices) {
    // Write your solution here

    return 0;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    int[] nums = new int[n];
    String[] parts = sc.nextLine().trim().split("\\s+");
    for (int i = 0; i < n; i++) nums[i] = Integer.parseInt(parts[i]);
    System.out.print(maxProfit(nums));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Best Time to Buy and Sell Stock');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('6
7 1 5 3 6 4', '5', false),
('5
7 6 4 3 1', '0', false),
('2
1 2', '1', false),
('3
2 4 1', '2', true),
('3
3 3 3', '0', true),
('6
3 2 6 5 0 3', '4', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Best Time to Buy and Sell Stock'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Product of Array Except Self (MEDIUM)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Product of Array Except Self',
'Given an integer array nums, return an array answer such that answer[i] is equal to the product of all the elements of nums except nums[i].

Do not use division, and aim for O(n) time. All products fit in a 32-bit integer.

Input: the first line is n, the second line contains the n integers separated by spaces.
Output: the n values of answer separated by single spaces.',
'MEDIUM',
'public static int[] productExceptSelf(int[] nums) {
    // Write your solution here

    return new int[]{};
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    int[] nums = new int[n];
    String[] parts = sc.nextLine().trim().split("\\s+");
    for (int i = 0; i < n; i++) nums[i] = Integer.parseInt(parts[i]);
    int[] result = productExceptSelf(nums);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < result.length; i++) {
        if (i > 0) sb.append(" ");
        sb.append(result[i]);
    }
    System.out.print(sb.toString());
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Product of Array Except Self');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('4
1 2 3 4', '24 12 8 6', false),
('5
-1 1 0 -3 3', '0 0 9 0 0', false),
('2
2 3', '3 2', false),
('4
1 1 1 1', '1 1 1 1', true),
('3
0 0 2', '0 0 0', true),
('4
5 -2 4 3', '-24 60 -30 -40', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Product of Array Except Self'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Coin Change (MEDIUM)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Coin Change',
'You are given an integer array coins representing coins of different denominations and an integer amount.

Return the fewest number of coins needed to make up that amount. If it cannot be made up by any combination of the coins, return -1. You may use each coin denomination an unlimited number of times.

Input: the first line is n, the number of denominations; the second line contains the n coin values separated by spaces; the third line contains amount.
Output: the fewest number of coins, or -1.',
'MEDIUM',
'public static int coinChange(int[] coins, int amount) {
    // Write your solution here

    return -1;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    int[] coins = new int[n];
    String[] parts = sc.nextLine().trim().split("\\s+");
    for (int i = 0; i < n; i++) coins[i] = Integer.parseInt(parts[i]);
    int amount = Integer.parseInt(sc.nextLine().trim());
    System.out.print(coinChange(coins, amount));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Coin Change');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('3
1 2 5
11', '3', false),
('1
2
3', '-1', false),
('1
1
0', '0', false),
('3
1 3 4
6', '2', true),
('4
186 419 83 408
6249', '20', true),
('2
5 10
3', '-1', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Coin Change'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Longest Substring Without Repeating Characters (MEDIUM)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Longest Substring Without Repeating Characters',
'Given a string s, find the length of the longest substring that contains no repeating characters.

Input: a single non-empty line containing s (letters and digits only, no spaces).
Output: the length of the longest such substring.',
'MEDIUM',
'public static int lengthOfLongestSubstring(String s) {
    // Write your solution here

    return 0;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    String s = sc.nextLine().trim();
    System.out.print(lengthOfLongestSubstring(s));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Longest Substring Without Repeating Characters');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('abcabcbb', '3', false),
('bbbbb', '1', false),
('pwwkew', '3', false),
('abcdefg', '7', true),
('dvdf', '3', true),
('a1b2c3a1b2', '6', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Longest Substring Without Repeating Characters'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);

-- Trapping Rain Water (HARD)
INSERT INTO problem (title, description, difficulty, solution_template, driver_code)
SELECT 'Trapping Rain Water',
'Given n non-negative integers representing an elevation map where the width of each bar is 1, compute how much water it can trap after raining.

Input: the first line is n, the second line contains the n heights separated by spaces.
Output: the total units of trapped water.',
'HARD',
'public static int trap(int[] height) {
    // Write your solution here

    return 0;
}',
'public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int n = Integer.parseInt(sc.nextLine().trim());
    int[] nums = new int[n];
    String[] parts = sc.nextLine().trim().split("\\s+");
    for (int i = 0; i < n; i++) nums[i] = Integer.parseInt(parts[i]);
    System.out.print(trap(nums));
}'
WHERE NOT EXISTS (SELECT 1 FROM problem WHERE title = 'Trapping Rain Water');

INSERT INTO test_case (input, expected_output, hidden, problem_id)
SELECT v.input, v.expected_output, v.hidden, p.id
FROM (VALUES
('12
0 1 0 2 1 0 1 3 2 1 2 1', '6', false),
('6
4 2 0 3 2 5', '9', false),
('3
1 2 3', '0', false),
('6
3 0 0 2 0 4', '10', true),
('4
5 4 1 2', '1', true),
('3
2 0 2', '2', true)
) AS v(input, expected_output, hidden), problem p
WHERE p.title = 'Trapping Rain Water'
  AND NOT EXISTS (SELECT 1 FROM test_case tc WHERE tc.problem_id = p.id);
