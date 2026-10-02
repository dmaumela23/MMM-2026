package com.maumela.magnummanagement.utils

import com.maumela.magnummanagement.data.model.ProductDto
import com.maumela.magnummanagement.data.model.ServiceDto
import com.maumela.magnummanagement.data.model.UserDto
import com.maumela.magnummanagement.data.model.UserRole

/**
 * Which buttons a role may SEE. The server enforces the same rules on every request,
 * so hiding a button is a convenience, never the security.
 */
object Permissions {
    fun canCreateProducts(role: UserRole?): Boolean = role == UserRole.BUSINESS
    fun canCreateServices(role: UserRole?): Boolean = role == UserRole.PROVIDER || role == UserRole.BUSINESS

    /** Seller roles receive incoming order lines and can advance their status. */
    fun isSeller(role: UserRole?): Boolean = canCreateServices(role)

    fun ownsProduct(user: UserDto?, product: ProductDto): Boolean =
        user != null && canCreateProducts(user.role) && product.sellerId == user.id

    fun ownsService(user: UserDto?, service: ServiceDto): Boolean =
        user != null && canCreateServices(user.role) && service.providerId == user.id
}
