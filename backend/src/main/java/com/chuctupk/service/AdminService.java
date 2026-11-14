package com.chuctupk.service;

import com.chuctupk.dto.request.*;
import com.chuctupk.dto.response.*;
import com.chuctupk.entity.User;

import java.util.Map;

public interface AdminService {

    AdminDashboardResponse getDashboard();

    AdminAccountListResponse getAccounts(String q, String role, int page, int limit);

    AccountDto getAccount(Long id);

    AccountDto createAccount(AdminCreateAccountRequest request);

    AccountDto updateAccount(Long id, AdminUpdateAccountRequest request);

    AccountDto changeRole(Long id, AdminChangeRoleRequest request);

    AccountDto setAccountActive(User adminUser, Long id, boolean active);

    void resetPassword(Long id, AdminResetPasswordRequest request);

    PatientDto setPatientActive(Long patientId, boolean active);

    AdminPostListResponse getAdminPosts(String q, String category, String status, int page, int limit);

    PostDto getAdminPost(Long id);

    PostDto createAdminPost(User author, AdminSavePostRequest request);

    PostDto updateAdminPost(Long id, AdminSavePostRequest request);

    PostDto setPostPin(Long id, boolean pin);

    void deletePost(Long id);

    PublicPostListResponse getPublicPosts(String category, int page, int limit, boolean includeFeatured);

    PostDto getPublicPost(String slugOrId);

    Map<String, Object> saveUpload(String contentType, byte[] data);

    byte[] getUpload(String id);

    String getUploadContentType(String id);
}

