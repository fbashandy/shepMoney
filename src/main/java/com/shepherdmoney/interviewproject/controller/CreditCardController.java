package com.shepherdmoney.interviewproject.controller;

import com.shepherdmoney.interviewproject.model.BalanceHistory;
import com.shepherdmoney.interviewproject.model.CreditCard;
import com.shepherdmoney.interviewproject.model.User;
import com.shepherdmoney.interviewproject.repository.CreditCardRepository;
import com.shepherdmoney.interviewproject.repository.UserRepository;

import com.shepherdmoney.interviewproject.vo.request.AddCreditCardToUserPayload;
import com.shepherdmoney.interviewproject.vo.request.UpdateBalancePayload;
import com.shepherdmoney.interviewproject.vo.response.CreditCardView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDate;
import java.util.TreeMap;


@RestController
public class CreditCardController {

    // TODO: wire in CreditCard repository here (~1 line)
    @Autowired
    private CreditCardRepository creditCardRepository;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/credit-card")
    public ResponseEntity<Integer> addCreditCardToUser(@RequestBody AddCreditCardToUserPayload payload) {
        // TODO: Create a credit card entity, and then associate that credit card with user with given userId
        //       Return 200 OK with the credit card id if the user exists and credit card is successfully associated with the user
        //       Return other appropriate response code for other exception cases
        //       Do not worry about validating the card number, assume card number could be any arbitrary format and length
        // look
        Optional<User> user = userRepository.findById(payload.getUserId());
        if (user.isPresent()) {
            //present
            CreditCard creditCard = new CreditCard();
            creditCard.setIssuanceBank(payload.getCardIssuanceBank());
            creditCard.setNumber(payload.getCardNumber());
            creditCard.setUser(user.get());
            creditCard = creditCardRepository.save(creditCard);
            return ResponseEntity.ok(creditCard.getId());
        } else {
            // not present
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/credit-card:all")
    public ResponseEntity<List<CreditCardView>> getAllCardOfUser(@RequestParam int userId) {
        // TODO: return a list of all credit card associated with the given userId, using CreditCardView class
        //       if the user has no credit card, return empty list, never return null


        Optional<User> userOptional = userRepository.findById(userId);
        if (!userOptional.isPresent()) {
            return ResponseEntity.ok(new ArrayList<>()); // return empty if DNE
        }

        User user = userOptional.get();
        List<CreditCard> creditCards = new ArrayList<>(user.getCreditCards());
        List<CreditCardView> creditCardViews = new ArrayList<>();

        for (CreditCard card : creditCards) {
            CreditCardView view = new CreditCardView(card.getIssuanceBank(), card.getNumber());
            creditCardViews.add(view);
        }

        return ResponseEntity.ok(creditCardViews);

    }

    @GetMapping("/credit-card:user-id")
    public ResponseEntity<Integer> getUserIdForCreditCard(@RequestParam String creditCardNumber) {
        // TODO: Given a credit card number, efficiently find whether there is a user associated with the credit card
        //       If so, return the user id in a 200 OK response. If no such user exists, return 400 Bad Request
        List<CreditCard> allCards = creditCardRepository.findAll(); // get allcards
        Optional<CreditCard> matchingCard = allCards.stream()
                .filter(card -> card.getNumber().equals(creditCardNumber))
                .findFirst();
        if (matchingCard.isPresent()) {
            // card
            User user = matchingCard.get().getUser();
            if (user != null) {
                return ResponseEntity.ok(user.getId());
            } else {
                // no user
                return ResponseEntity.badRequest().body(null);
            }
        } else {
            // DNE
            return ResponseEntity.badRequest().body(null);
        }

    }

    @PostMapping("/credit-card:update-balance")
    public ResponseEntity<String> updateBalance(@RequestBody UpdateBalancePayload[] payload) {
        //TODO: Given a list of transactions, update credit cards' balance history.
        //      1. For the balance history in the credit card
        //      2. If there are gaps between two balance dates, fill the empty date with the balance of the previous date
        //      3. Given the payload `payload`, calculate the balance different between the payload and the actual balance stored in the database
        //      4. If the different is not 0, update all the following budget with the difference
        //      For example: if today is 4/12, a credit card's balanceHistory is [{date: 4/12, balance: 110}, {date: 4/10, balance: 100}],
        //      Given a balance amount of {date: 4/11, amount: 110}, the new balanceHistory is
        //      [{date: 4/12, balance: 120}, {date: 4/11, balance: 110}, {date: 4/10, balance: 100}]
        //      This is because
        //      1. You would first populate 4/11 with previous day's balance (4/10), so {date: 4/11, amount: 100}
        //      2. And then you observe there is a +10 difference
        //      3. You propagate that +10 difference until today
        //      Return 200 OK if update is done and successful, 400 Bad Request if the given card number
        //        is not associated with a card.
        for (UpdateBalancePayload updatePayload : payload) {
            // find card
            Optional<CreditCard> cardOptional = creditCardRepository.findAll().stream()
                    .filter(card -> card.getNumber().equals(updatePayload.getCreditCardNumber()))
                    .findFirst();

            if (!cardOptional.isPresent()) {
                return ResponseEntity.badRequest().body("Credit card not found with number: " + updatePayload.getCreditCardNumber());
            }

            CreditCard creditCard = cardOptional.get();
            List<BalanceHistory> histories = creditCard.getBalanceHistories();

            //  create one
            LocalDate updateDate = updatePayload.getBalanceDate();
            Optional<BalanceHistory> existingHistoryOpt = histories.stream()
                    .filter(history -> history.getDate().isEqual(updateDate))
                    .findFirst();

            if (existingHistoryOpt.isPresent()) {
                // update
                BalanceHistory existingHistory = existingHistoryOpt.get();
                existingHistory.setBalance(existingHistory.getBalance() + updatePayload.getBalanceAmount());
            } else {
                // copy
                BalanceHistory newHistory = new BalanceHistory();
                newHistory.setDate(updateDate);
                newHistory.setBalance(updatePayload.getBalanceAmount());
                newHistory.setCreditCard(creditCard);
                histories.add(newHistory);
                // order
                histories.sort(Comparator.comparing(BalanceHistory::getDate).reversed());
            }

            // save
            creditCardRepository.save(creditCard);
        }

        return ResponseEntity.ok("All balances updated successfully.");
    }
    
}
