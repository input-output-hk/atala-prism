all:
	pdflatex uas.tex
	bibtex uas
	pdflatex uas.tex
	pdflatex uas.tex

paper:
	pdflatex uas-paper.tex
	bibtex uas-paper
	pdflatex uas-paper.tex
	pdflatex uas-paper.tex

clean:
	rm *~ *.aux *.log *.blg *.bbl *.out
