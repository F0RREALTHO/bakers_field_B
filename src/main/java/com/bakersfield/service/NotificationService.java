package com.bakersfield.service;

import com.bakersfield.model.Order;
import com.bakersfield.model.CustomOrder;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import java.util.Base64;

@Service
public class NotificationService {
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${admin.notification.email:karthik@example.com}")
    private String adminEmail;

    @Value("${admin.notification.whatsapp_number:919876543210}")
    private String adminPhone;

    @Value("${admin.notification.twilio_account_sid:}")
    private String twilioSid;

    @Value("${admin.notification.twilio_auth_token:}")
    private String twilioToken;

    @Value("${admin.notification.twilio_phone_number:whatsapp:+14155238886}")
    private String twilioFrom;

    private final RestTemplate restTemplate = new RestTemplate();

    public NotificationService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendOwnerOtpEmail(String to, String otp) {
        String subject = "BakersField Owner Login OTP";
        String body = "Your OTP for owner login is: " + otp + "\n\nThis OTP expires in 5 minutes.";
        sendEmail(to, subject, body);
    }

    @Async
    public void sendOrderNotification(Order order) {
        try {
            String subject = "New Order Recieved: #" + order.getId();
            String body = String.format(
                "New order received from %s (%s)\n" +
                "Total Amount: ₹%.2f\n" +
                "Payment Status: %s\n" +
                "Address: %s, %s",
                order.getCustomerName(),
                order.getPhoneNumber(),
                order.getTotalAmountInr(),
                order.getPaymentStatus(),
                order.getAddressLine1(),
                order.getAddressCity()
            );

            sendEmail(adminEmail, subject, body);
            
            // Notification to Admin
            String customerPhone = order.getPhoneNumber().replaceAll("[^0-9]", "");
            if (customerPhone.length() == 10) customerPhone = "91" + customerPhone;
            
            StringBuilder adminMsg = new StringBuilder();
            adminMsg.append("🧾 *New Order #").append(order.getId()).append("*\n\n");
            adminMsg.append("*Customer:* ").append(order.getCustomerName()).append("\n");
            
            String displayPhone = order.getPhoneNumber().replaceAll("[^0-9]", "");
            if (displayPhone.length() == 10) displayPhone = "91" + displayPhone;
            adminMsg.append("*Phone:* +").append(displayPhone).append("\n\n");
            
            adminMsg.append("*Items:*\n");
            for (com.bakersfield.model.OrderItem item : order.getItems()) {
                adminMsg.append("- ").append(item.getItemName()).append(" x").append(item.getQuantity())
                        .append(" (₹").append(item.getUnitPriceInr()).append(")\n");
            }
            adminMsg.append("\n");
            
            adminMsg.append("*Summary:*\n");
            adminMsg.append("Subtotal: ₹").append(order.getSubtotalAmountInr()).append("\n");
            if (order.getSaleDiscountAmountInr() != null && order.getSaleDiscountAmountInr().compareTo(java.math.BigDecimal.ZERO) > 0) {
                adminMsg.append("Sale Discount (").append(order.getSaleName()).append("): -₹").append(order.getSaleDiscountAmountInr()).append("\n");
            }
            if (order.getDiscountAmountInr() != null && order.getDiscountAmountInr().compareTo(java.math.BigDecimal.ZERO) > 0) {
                adminMsg.append("Coupon Discount: -₹").append(order.getDiscountAmountInr()).append("\n");
            }
            if (order.getCouponCode() != null) {
                adminMsg.append("Coupon: ").append(order.getCouponCode()).append("\n");
            }
            adminMsg.append("*Total: ₹").append(order.getTotalAmountInr()).append("*\n\n");
            
            adminMsg.append("*Delivery Address:*\n");
            if (order.getAddressLabel() != null) adminMsg.append("[").append(order.getAddressLabel()).append("] ");
            adminMsg.append(order.getAddressLine1()).append(", ");
            if (order.getAddressLine2() != null && !order.getAddressLine2().isEmpty()) {
                adminMsg.append(order.getAddressLine2()).append(", ");
            }
            adminMsg.append(order.getAddressCity()).append(", ").append(order.getAddressState()).append(" - ").append(order.getPinCode()).append("\n\n");
            
            adminMsg.append("*Payment:* ").append(order.getPaymentMethod()).append(" (").append(order.getPaymentStatus()).append(")\n\n");
            adminMsg.append("*Quick Chat:* https://wa.me/").append(customerPhone);
            
            sendWhatsAppMessage(adminPhone, adminMsg.toString(), null);

            // Notification to Customer
            String customerMsg = String.format("Hi %s, your BakersField order #%d (₹%.2f) has been placed successfully! We will notify you when it's out for delivery.",
                order.getCustomerName(), order.getId(), order.getTotalAmountInr());
            sendWhatsAppMessage(order.getPhoneNumber(), customerMsg, null);
        } catch (Exception e) {
            logger.error("Failed to send order notification", e);
        }
    }

    @Async
    public void sendCustomOrderNotification(CustomOrder order) {
        try {
            String subject = "New Custom Cake Request: #" + order.getId();
            String body = String.format(
                "New custom cake request from %s (%s)\n" +
                "Occasion: %s\n" +
                "Description: %s\n" +
                "Estimated Budget: ₹%.2f",
                order.getCustomerName(),
                order.getPhoneNumber(),
                order.getOccasion(),
                order.getDescription(),
                order.getEstimatedPriceInr()
            );

            sendEmail(adminEmail, subject, body);

            // Notification to Admin
            String customerPhone = order.getPhoneNumber().replaceAll("[^0-9]", "");
            if (customerPhone.length() == 10) customerPhone = "91" + customerPhone;

            StringBuilder adminMsg = new StringBuilder();
            adminMsg.append("🎂 *New Custom Cake Request #").append(order.getId()).append("*\n\n");
            adminMsg.append("*Customer:* ").append(order.getCustomerName()).append("\n");
            
            String displayPhone = order.getPhoneNumber().replaceAll("[^0-9]", "");
            if (displayPhone.length() == 10) displayPhone = "91" + displayPhone;
            adminMsg.append("*Phone:* +").append(displayPhone).append("\n\n");
            
            adminMsg.append("*Occasion:* ").append(order.getOccasion()).append("\n");
            adminMsg.append("*Budget:* ₹").append(order.getEstimatedPriceInr()).append("\n");
            adminMsg.append("*Date Requested:* ").append(order.getRequestedFor().toString()).append("\n\n");
            
            adminMsg.append("*Description:*\n").append(order.getDescription()).append("\n\n");
            
            adminMsg.append("*Quick Chat:* https://wa.me/").append(customerPhone);
            
            sendWhatsAppMessage(adminPhone, adminMsg.toString(), null);

            // Notification to Customer
            String customerMsg = String.format("Hi %s, thank you for your custom cake request for %s! Our team will contact you on this number within 2 hours to finalize the design.",
                order.getCustomerName(), order.getOccasion());
            sendWhatsAppMessage(order.getPhoneNumber(), customerMsg, null);
        } catch (Exception e) {
            logger.error("Failed to send custom order notification", e);
        }
    }

    private void sendEmail(String to, String subject, String text) {
        if (fromEmail == null || fromEmail.isEmpty()) {
            logger.warn("Email notification skipped: spring.mail.username not configured");
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        logger.info("Email notification sent to {}", to);
    }

    private void sendWhatsAppMessage(String phone, String messageContent, String mediaUrl) {
        if (twilioSid == null || twilioSid.isEmpty() || twilioToken == null || twilioToken.isEmpty()) {
            logger.warn("WhatsApp notification skipped: Twilio credentials not configured");
            return;
        }

        try {
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            
            // Smarter logic: if it's 10 digits and doesn't start with 91, add 91.
            // If it's 10 digits and ALREADY starts with 91, it might be an 8-digit number (rare) 
            // OR the user entered 91 as part of 10 digits. 
            // Most Indian mobiles are 10 digits.
            if (cleanPhone.length() == 10) {
                // If it starts with 91, we check if it's possibly already a country-coded number or just a 10-digit number.
                // To be safe, if length is 10, we prepend 91.
                cleanPhone = "91" + cleanPhone;
            } else if (cleanPhone.length() == 12 && cleanPhone.startsWith("0")) {
                cleanPhone = cleanPhone.substring(1);
            }
            
            String finalRecipient = "whatsapp:+" + cleanPhone;
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + twilioSid + "/Messages.json";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String auth = twilioSid + ":" + twilioToken;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
            headers.set("Authorization", "Basic " + encodedAuth);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("From", twilioFrom);
            map.add("To", "whatsapp:+" + cleanPhone);
            map.add("Body", messageContent);
            if (mediaUrl != null && !mediaUrl.isEmpty()) {
                map.add("MediaUrl", mediaUrl);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            logger.info("WhatsApp notification response for {}: {}", finalRecipient, response.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            logger.error("Twilio API Error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            logger.error("Failed to send WhatsApp message via Twilio", e);
        }
    }
}
