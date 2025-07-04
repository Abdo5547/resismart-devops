package com.resismart.RESISMART.controller;

import com.resismart.RESISMART.dto.PaymentDTO;
import com.resismart.RESISMART.dto.PaymentDTOv2;
import com.resismart.RESISMART.models.Payment;
import com.resismart.RESISMART.models.PaymentHistorie;
import com.resismart.RESISMART.models.Resident;
import com.resismart.RESISMART.repository.PaymentHistorieRepository;
import com.resismart.RESISMART.repository.PaymentRepository;
import com.resismart.RESISMART.repository.ResidentRepository;
import com.resismart.RESISMART.service.PaymentService;
import com.resismart.RESISMART.statistique.PaymentStats;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;
    
    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ResidentRepository residentRepository;


    @Autowired
    private PaymentHistorieRepository paymentHistorieRepository;

    //recupere la liste de tous les paiements
    @GetMapping("/allpayments")
    public List<PaymentDTO> getAllPayments() {
        return paymentService.getAllPayments();
    }

    //recupere la liste des paiements d'un residant
    @GetMapping("/resident/{residentId}")
    public List<PaymentDTO> getPaymentsByResident(@PathVariable Integer residentId) {
        Resident resident = new Resident();
        resident.setId(residentId);  // Suppose qu'on a une méthode pour récupérer le résident par son ID
        return paymentService.getPaymentsByResident(resident);
    }

    //recupere les statistiques des paiements
    @GetMapping("/statistiques")
    public Map<String, Object> getPaymentsStatistics() {
        PaymentStats paymentStats = paymentService.getPaymentStatistics();
        Map<String, Object> stats = new HashMap<>();
        stats.put("completedPayments", paymentStats.getCompletedPayments());
        stats.put("pendingPayments", paymentStats.getPendingPayments());
        stats.put("LatePayments", paymentStats.getLatePayments());  // Ajout des paiements en retard
        return stats;
    }

    @PutMapping("/update/{residentId}")
    public String updatePaymentStatus(@PathVariable Integer residentId, @RequestBody PaymentDTOv2 paymentDTO) {
        // Vérifier si le résident existe
        Resident resident = residentRepository.findById(residentId).orElse(null);
        if (resident == null) {
            return "Resident not found";
        }

        // Chercher tous les paiements du résident
        List<Payment> payments = paymentRepository.findByResident(resident);
        if (payments.isEmpty()) {
            return "No payments found for the resident";
        }

        // Mettre à jour le premier paiement trouvé (ou choisir selon un critère spécifique)
        Payment paymentToUpdate = payments.get(0); // Par exemple, on prend le premier paiement trouvé
        if (paymentDTO.getStatus() != null) {
            paymentToUpdate.setStatus(paymentDTO.getStatus());
        }

        // Sauvegarder les modifications
        paymentRepository.save(paymentToUpdate);


         PaymentHistorie paymentHistorie = new PaymentHistorie();
         paymentHistorie.setResident(resident);
         paymentHistorie.setDatePayment(new java.sql.Date(System.currentTimeMillis())); // Date actuelle

          // Sauvegarder l'historique du paiement
          paymentHistorieRepository.save(paymentHistorie);

            // Vérification du stockage
         if (paymentHistorie.getId() == null) {
            return "Failed to save payment history";
         }

        return "Payment status updated successfully";
    }

    // Marquer un paiement comme payé
 @PostMapping("/{id}/pay")
    public ResponseEntity<String> pay(@PathVariable("id") Integer paymentId) {
        try {
            paymentService.markPaymentAsPaid(paymentId);
            return ResponseEntity.ok("Paiement mis à jour avec succès.");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


// 1. Réinitialiser le statut de tous les paiements en "PendingPayment" avec isSet = false
 @PutMapping("/pendingpayment")
    public String resetPaymentStatus() {
        List<Payment> payments = paymentRepository.findAll();
        for (Payment payment : payments) {
            payment.setStatus("PendingPayment");
            payment.setIsSet(false);
        }
        paymentRepository.saveAll(payments);
        return "Tous les paiements ont été mis à jour en PendingPayment avec isSet = false";
    }

    // 2. Changer le statut des paiements où isSet est false en "LatePayment"
 @PutMapping("/latepayment")
    public String markLatePayments() {
        List<Payment> payments = paymentRepository.findByIsSetFalse();
        for (Payment payment : payments) {
            payment.setStatus("LatePayment");
        }
        paymentRepository.saveAll(payments);
        return "Tous les paiements non réglés ont été marqués comme LatePayment";
    }
}
