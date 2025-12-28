import { Routes } from '@angular/router';
import { Login } from './components/auth/login/login';
import { Register } from './components/auth/register/register';
import { Dashboard } from './components/dashboard/dashboard';
import { AuthGuard } from './services/auth/auth-guard';
import { MapComponent } from './components/map/map.component';
import { RegisterRequestForm } from './components/register-request-form/register-request-form';

export const routes: Routes = [
    { path: '', component: Login },
    { path: 'register', component: Register },
    {
        path: 'app',
        component: Dashboard,
        canActivate: [AuthGuard],
        children: [
            { path: '', redirectTo: 'home', pathMatch: 'full' },
            {
                path: 'home',
                component: RegisterRequestForm
            }
        ]
    },
];
