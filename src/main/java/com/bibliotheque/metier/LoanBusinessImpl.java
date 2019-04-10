package com.bibliotheque.metier;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;

import com.bibliotheque.dao.LoanRepository;
import com.bibliotheque.entities.Book;
import com.bibliotheque.entities.Loan;
import com.bibliotheque.entities.User;
import com.bibliotheque.exception.BibliothequeException;
import com.bibliotheque.exception.BibliothequeFault;

@Service
@PropertySource("classpath:bibliotheque.properties")
public class LoanBusinessImpl implements LoanBusiness {

	@Autowired
	private LoanRepository loanRepository;
	@Autowired
	private BookBusiness bookBusiness;
	@Autowired
	private UserBusiness userBusiness;
	@Value("${prolongation.days}")
	private int extendDays;
	@Value("${loan.days}")
	private int loanDays;
		
	@Override
	public void extendLoan(Long loan_ID, Long user_ID) throws BibliothequeException {
		Loan r = loanRepository.findById(loan_ID).orElse(null);

		if (r == null) {

			BibliothequeFault bibliothequeFault = new BibliothequeFault();
			bibliothequeFault.setFaultCode("30");
			bibliothequeFault.setFaultString("ID reservation incorrect");

			throw new BibliothequeException("ID return null", bibliothequeFault);

		} else if (r.getUser().getId() != user_ID) {

			BibliothequeFault bibliothequeFault = new BibliothequeFault();
			bibliothequeFault.setFaultCode("31");
			bibliothequeFault.setFaultString("ID utilisateur incorrect");

			throw new BibliothequeException("ID utilisateur incorrect", bibliothequeFault);

		} else if (r.isExtension()) {

			BibliothequeFault bibliothequeFault = new BibliothequeFault();
			bibliothequeFault.setFaultCode("32");
			bibliothequeFault.setFaultString("Réservation déjà prolonger");

			throw new BibliothequeException("Prolongation true", bibliothequeFault);

		} else if (checkLate(r)) {

			BibliothequeFault bibliothequeFault = new BibliothequeFault();
			bibliothequeFault.setFaultCode("33");
			bibliothequeFault.setFaultString("Retard après prolongation");

			throw new BibliothequeException("Retard après prolongation", bibliothequeFault);
		}

		Calendar c = Calendar.getInstance();
		c.add(Calendar.DATE, extendDays);
		r.setEnd_loan(c.getTime());
		r.setExtension(true);

		loanRepository.save(r);
	}

	@Override
	public void returnLoan(Long id) {
		Loan loan = loanRepository.findById(id).orElse(null);
		loan.setMade(true);		
		loanRepository.save(loan);
	}

	@Override
	public Loan getLoan(Long id) {
		return loanRepository.findById(id).orElse(null);
	}

	@Override
	public void createLoan(Long book_id, Long User_id)
			throws BibliothequeException, ParseException {
		User user = userBusiness.getUser(User_id);
		Book book = bookBusiness.getBook(book_id);

		if (user == null) {
			BibliothequeFault bibliothequeFault = new BibliothequeFault();
			bibliothequeFault.setFaultCode("12");
			bibliothequeFault.setFaultString("ID utilisateur incorrect");

			throw new BibliothequeException("ID return null", bibliothequeFault);

		} else if (book == null) {
			BibliothequeFault bibliothequeFault = new BibliothequeFault();
			bibliothequeFault.setFaultCode("13");
			bibliothequeFault.setFaultString("ID ouvrage incorrect");

			throw new BibliothequeException("ID return null", bibliothequeFault);
		}

		if (book.getCopyAvailable() == 0) {
			BibliothequeFault bibliothequeFault = new BibliothequeFault();
			bibliothequeFault.setFaultCode("14");
			bibliothequeFault.setFaultString("Ouvrage non disponible au prêt pour cette période");

			throw new BibliothequeException("Non disponible", bibliothequeFault);
		}

		book.setCopyAvailable(book.getCopyAvailable() - 1);

		if (book.getCopyAvailable() == 0)
			book.setAvailable(false);

		Calendar c = Calendar.getInstance();
//		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
//		c.setTime(sdf.parse("2018-01-25"));
		Date start_loan = c.getTime();
		
		c.add(Calendar.DATE, loanDays); // Adding 5 days
		Date end_loan = c.getTime();
				
		bookBusiness.saveBook(book);
		loanRepository.save(new Loan(start_loan, end_loan, user, book));
	}

	@Override
	public List<Loan> getListLoanByUserID(Long user_id) {
		return loanRepository.getListLoanByUserID(user_id, new Date());
	}

	@Override
	public List<Loan> getListLoanLateByUserID(Long user_id) {

		List<Loan> list = loanRepository.getListLoanLateByUserID(user_id,
				new Date());

		for (Loan loan : list) {
			if (!loan.isExtension()) {

				if (checkLate(loan))
					loan.setLate(true);
			}
		}
		return list;
	}

	public boolean checkLate(Loan loan) {
		Calendar c = Calendar.getInstance();
		c.setTime(loan.getEnd_loan());
		c.add(Calendar.DATE, extendDays);
		Date retard = c.getTime();

		if (retard.before(new Date())) {
			return true;
		}
		return false;
	}

	@Override
	public int getDaysExtend() {
		return extendDays;
	}

	@Override
	public int getDaysLoan() {		
		return loanDays;
	}

}
