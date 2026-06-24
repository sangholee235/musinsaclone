package com.musinsaclone.user.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.musinsaclone.common.exception.BusinessException;
import com.musinsaclone.coupon.repository.UserCouponRepository;
import com.musinsaclone.point.repository.PointHistoryRepository;
import com.musinsaclone.user.entity.Address;
import com.musinsaclone.user.entity.User;
import com.musinsaclone.user.repository.AddressRepository;
import com.musinsaclone.user.repository.UserRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final UserCouponRepository userCouponRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        return new UserProfileResponse(user);
    }

    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        String name = request.getName();
        if (name == null || name.isBlank()) throw BusinessException.badRequest("이름을 입력해주세요.");
        user.updateProfile(name.trim(), request.getPhone());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다."));
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw BusinessException.badRequest("새 비밀번호는 8자 이상이어야 합니다.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw BusinessException.badRequest("현재 비밀번호가 일치하지 않습니다.");
        }
        user.changePassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return addressRepository.findByUserId(userId).stream().map(AddressResponse::new).toList();
    }

    @Transactional
    public void addAddress(Long userId, AddressRequest request) {
        User user = userRepository.getReferenceById(userId);
        if (request.isDefault()) {
            addressRepository.findByUserIdAndIsDefault(userId, true)
                    .ifPresent(a -> addressRepository.save(withDefault(a, false)));
        }
        addressRepository.save(Address.builder()
                .user(user)
                .name(request.getName())
                .recipient(request.getRecipient())
                .phone(request.getPhone())
                .zipcode(request.getZipcode())
                .address1(request.getAddress1())
                .address2(request.getAddress2())
                .isDefault(request.isDefault())
                .build());
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> BusinessException.notFound("주소를 찾을 수 없습니다."));
        if (!address.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        addressRepository.delete(address);
    }

    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        Address target = addressRepository.findById(addressId)
                .orElseThrow(() -> BusinessException.notFound("주소를 찾을 수 없습니다."));
        if (!target.getUser().getId().equals(userId)) throw BusinessException.forbidden("권한이 없습니다.");
        if (target.isDefault()) return;
        addressRepository.findByUserIdAndIsDefault(userId, true)
                .ifPresent(a -> addressRepository.save(withDefault(a, false)));
        addressRepository.save(withDefault(target, true));
    }

    private Address withDefault(Address a, boolean isDefault) {
        return Address.builder()
                .id(a.getId()).user(a.getUser()).name(a.getName())
                .recipient(a.getRecipient()).phone(a.getPhone())
                .zipcode(a.getZipcode()).address1(a.getAddress1())
                .address2(a.getAddress2()).isDefault(isDefault).build();
    }

    @Getter
    @Setter
    public static class UpdateProfileRequest {
        private String name;
        private String phone;
    }

    @Getter
    @Setter
    public static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }

    @Getter
    public static class UserProfileResponse {
        private final Long id;
        private final String email;
        private final String name;
        private final String phone;
        private final int point;
        private final String role;

        public UserProfileResponse(User user) {
            this.id = user.getId();
            this.email = user.getEmail();
            this.name = user.getName();
            this.phone = user.getPhone();
            this.point = user.getPoint();
            this.role = user.getRole().name();
        }
    }

    @Getter
    public static class AddressResponse {
        private final Long id;
        private final String name;
        private final String recipient;
        private final String phone;
        private final String zipcode;
        private final String address1;
        private final String address2;
        private final boolean isDefault;

        public AddressResponse(Address address) {
            this.id = address.getId();
            this.name = address.getName();
            this.recipient = address.getRecipient();
            this.phone = address.getPhone();
            this.zipcode = address.getZipcode();
            this.address1 = address.getAddress1();
            this.address2 = address.getAddress2();
            this.isDefault = address.isDefault();
        }

        // boolean 'isDefault' 필드는 Jackson 기본 규칙상 'default' 로 직렬화되므로 getter 에 명시한다.
        @JsonProperty("isDefault")
        public boolean isDefault() {
            return isDefault;
        }
    }

    @Getter
    @Setter
    public static class AddressRequest {
        private String name;
        private String recipient;
        private String phone;
        private String zipcode;
        private String address1;
        private String address2;
        @JsonProperty("isDefault")
        private boolean isDefault;
    }
}
