-- Create tables first
CREATE TABLE IF NOT EXISTS problem (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255),
    description TEXT,
    difficulty VARCHAR(255),
    solution_template TEXT,
    driver_code TEXT
);

CREATE TABLE IF NOT EXISTS test_case (
    id BIGSERIAL PRIMARY KEY,
    input VARCHAR(255),
    expected_output VARCHAR(255),
    hidden BOOLEAN,
    problem_id BIGINT REFERENCES problem(id)
);

-- Problem 1: Two Sum
INSERT INTO problem (id, title, description, difficulty, solution_template, driver_code) VALUES (
1,
'Two Sum',
'Given an array of integers nums and an integer target, return indices of the two numbers such that they add up to target.\n\nYou may assume that each input would have exactly one solution, and you may not use the same element twice.\n\nReturn the answer as an array of two integers.',
'EASY',
'public static int[] twoSum(int[] nums, int target) {\n    // Write your solution here\n    \n    return new int[]{};\n}',
'public static void main(String[] args) throws Exception {\n    Scanner sc = new Scanner(System.in);\n    int n = Integer.parseInt(sc.nextLine().trim());\n    String[] parts = sc.nextLine().trim().split(" ");\n    int[] nums = new int[n];\n    for (int i = 0; i < n; i++) nums[i] = Integer.parseInt(parts[i]);\n    int target = Integer.parseInt(sc.nextLine().trim());\n    int[] result = twoSum(nums, target);\n    StringBuilder sb = new StringBuilder();\n    for (int i = 0; i < result.length; i++) {\n        if (i > 0) sb.append(" ");\n        sb.append(result[i]);\n    }\n    System.out.print(sb.toString());\n}'
);

-- Test cases for Two Sum
INSERT INTO test_case (input, expected_output, hidden, problem_id) VALUES
('4\n2 7 11 15\n9', '0 1', false, 1),
('3\n3 2 4\n6', '1 2', false, 1),
('2\n3 3\n6', '0 1', true, 1);

-- Problem 2: Reverse String
INSERT INTO problem (id, title, description, difficulty, solution_template, driver_code) VALUES (
2,
'Reverse String',
'Write a function that reverses a string.\n\nThe input string is given as a single line.\n\nReturn the reversed string.',
'EASY',
'public static String reverseString(String s) {\n    // Write your solution here\n    \n    return "";\n}',
'public static void main(String[] args) throws Exception {\n    Scanner sc = new Scanner(System.in);\n    String s = sc.nextLine();\n    System.out.print(reverseString(s));\n}'
);

-- Test cases for Reverse String
INSERT INTO test_case (input, expected_output, hidden, problem_id) VALUES
('hello', 'olleh', false, 2),
('CodeArena', 'anerAedoC', false, 2);

-- Reset Sequences so auto-increment works for newly added problems later
SELECT setval('problem_id_seq', (SELECT MAX(id) FROM problem));
SELECT setval('test_case_id_seq', (SELECT MAX(id) FROM test_case));
