package com.sadok.sportivo.users;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.sadok.sportivo.users.dto.UpdateUserRequest;
import com.sadok.sportivo.users.dto.UserResponse;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

  UserResponse toResponse(User user);

  /**
   * Applies non-null fields from {@code request} onto an existing {@code user}
   * instance.
   * Fields not present in the request (id, username, role, timestamps) are left
   * unchanged.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "username", ignore = true)
  @Mapping(target = "role", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "updatedAt", ignore = true)
  void updateUserFromRequest(UpdateUserRequest request, @MappingTarget User user);
}
