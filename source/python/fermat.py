import math

def check_format(a,b,c,n):
    if n <= 2:
       print("Fermat was right")
       return
    if (math.pow(a,n)+math.pow(b,n) != math.pow(c,n)):
	check_format(a,b,c,n-1)
        print(n)
    else:
	print("Fermat was wrong")
	return
check_format(3,4,5,100)   

