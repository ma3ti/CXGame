## STRATEGY


## STARTING THE GAME AS FIRST

Put your checker in the center column if you're first.

Because a    checker in the center column allows you to make a 
Connect 4 in all possible directions, this is the best possible first move.


## STARTING THE GAME AS SECOND  

Play your checker on top of your opponent's if you're second.

By putting your checker on top of your opponent's checker in the same column, 
you can guarantee at least a draw.

This strategy works best if your opponent drops their first checker in the center column.
If they put it somewhere else, take the cen ter column to gain the upper hand.

## GENERAL STRATEGY

After the first move i'll try to follow my guide-strategy.
until no one can do a win (example if i must allign 6 marker to win and i've marked 6 times yet it is useless check 
if i or my adv can win) runs a semi-random strategy.
after k marker are on the table, i'll run my minimax strategy with singlemovewin and singlemoveblock

in pratica cerco una logica semi-random fino a quando io o il mio adv non abbiamo k-1 simboli in gioco.
al k-1 simbolo in gioco entrano le funzioni singoleMoveWIn/Block + il minimax 

All'algoritmo single moveBlock ho aggiunto una condizione if(i != j) per bloccare anche tutte le mosse con k-1 
simboli gia allineati, mettendo un mio simbolo direttamento sul k-esimo in fila.
Perchè singleMoveBlock prima funzionava che per ogni mia colonna che potevo scegliere, piazzava un marker adv 
in ogni colonna per verificare che con la mia mossa non sbloccasse la possibilita al mio adv di infilare il k-esimo marker,
ritornando cosi tutte le colonne che non avessere ritornato un vantaggio all'avversario.
con l'aggiunta di if(i != j) blocco direttamente la sequenza di k-1 simboli adv.

