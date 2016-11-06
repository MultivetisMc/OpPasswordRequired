package com.rezzedup.opguard.api;

import com.rezzedup.opguard.Password;

public interface PasswordHandler
{
    boolean hasPassword();
    
    boolean setPassword(Password password);
    
    Password getPassword();
    
    boolean removePassword(Password password);
    
    boolean check(Password password);
}
