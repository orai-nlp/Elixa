#
# Emoticon map file
#
# Las update: 2015/07/21
# 
# main source: http://www.geekca.com/2011/05/29/lista-de-emoticonos-iconos-emotivos-de-internet/
#
# Each line contains a Java regular expresion matching one or more emoticons, and the value to replace
# the emoticon with. Format:
# 
# emotRE \t correspondingValue
#
#
# Emoticons are mapped into the following group values:
#
#        * SMILEYEMOT - emoticons matching smiley faces
#	 * CRYEMOT - emoticons matching smiley faces
#	 * SHOCKEMOT - emoticons matching shocking faces
#	 * MUTEEMOT - emoticons matching mute faces
#	 * ANGRYEMOT - emoticons matching angry faces
#	 * KISSEMOT - emoticons matching kisses
#	 * SADEMOT - emoticons matching sad faces
#	 * 
#	 * IMPORTANT NOTE: it is up to the user to include the elements of this schema in the polarity lexicon, 
#	 *                 and assign them polarities.
#
#
#
# ==============================
#  LICENSE
# ==============================
#
# Copyright 2014 Elhuyar Fundazioa
#
# This file is part of EliXa.
#
#    EliXa is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
#
#    EliXa is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.

#    You should have received a copy of the GNU General Public License
#    along with EliXa.  If not, see <http://www.gnu.org/licenses/>.
#

		
\(-?:	SMILEYEMOT
:-?\)+	SMILEYEMOT
:[ ]*\)+	SMILEYEMOT
:\)+	SMILEYEMOT
\(:	SMILEYEMOT
=\)+	SMILEYEMOT
\(=	SMILEYEMOT    
:D+	SMILEYEMOT
:P+	SMILEYEMOT
XD+	SMILEYEMOT
xD+	SMILEYEMOT
=D+	SMILEYEMOT
n_n	SMILEYEMOT
>:]	SMILEYEMOT
:\'\)+	SMILEYEMOT
\^\^	SMILEYEMOT
;\)+	SMILEYEMOT
;-\)+	SMILEYEMOT
:\'\(	CRYEMOT
=\'\[	CRYEMOT
:_\(	CRYEMOT
T[_]T	CRYEMOT
TOT 	CRYEMOT
;_;	CRYEMOT            
:-\(	CRYEMOT
:\(	CRYEMOT
\)-?:	CRYEMOT
//\):	CRYEMOT
D:	CRYEMOT
Dx	CRYEMOT
\'n\'	CRYEMOT
=O+	SHOCKEMOT
:-O+	SHOCKEMOT
=8-O	SHOCKEMOT
[0O]_[0O]	SHOCKEMOT
:-X+	MUTEEMOT
X-:	MUTEEMOT
\'x\'	MUTEEMOT
ò_ó	ANGRYEMOT
[¬][¬]	ANGRYEMOT
-\.- O\.ó	ANGRYEMOT
o\.Ó	ANGRYEMOT
:S[^aA-zZ]	ANGRYEMOT
[¬] [¬]	ANGRYEMOT
[¬] [¬]\*	ANGRYEMOT
:\*	KISSEMOT
=\*	KISSEMOT
\^\*\^	KISSEMOT
o3o	KISSEMOT
<3	KISSEMOT
u\.u	SADEMOT
u_u	SADEMOT
U_U	SADEMOT
:-?\\	SADEMOT
:-?\(	SADEMOT		    
XC 	SADEMOT
\):-/	SADEMOT
/:	SADEMOT
