import re

file_path = r'd:\cabinet\src\main\resources\data.sql'

def clean_line(line):
    # Clean explicit Enum values with spaces
    patterns = {
        r"' PRIVATE '": "'PRIVATE'",
        r"' LAPISCINE '": "'LAPISCINE'",
        r"' AVAILABLE '": "'AVAILABLE'",
        r"' FULL '": "'FULL'",
        r"' OVERDUE '": "'OVERDUE'",
        r"' BROKEN '": "'BROKEN'",
        r"' DISABLED '": "'DISABLED'",
        r"' PENDING '": "'PENDING'",
    }
    
    for old, new in patterns.items():
        line = line.replace(old, new)
        
    # Clean Section strings ' Section X ' -> 'Section X'
    # Use Regex to capture explicit logic
    line = re.sub(r"' Section (\d+) '", r"'Section \1'", line)
    
    return line

try:
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    new_lines = [clean_line(line) for line in lines]
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
        
    print("Successfully cleaned data.sql")
    
except Exception as e:
    print(f"Error escaping: {e}")
