import { Routes } from '@angular/router';
import { Login } from './components/auth/login/login';
import { Register } from './components/auth/register/register';
import { Dashboard } from './components/dashboard/dashboard';
import { AuthGuard } from './services/auth/auth-guard';
import { MapComponent } from './components/map/map.component';
import { RegisterRequestForm } from './components/register-request-form/register-request-form';
import { ManagerGuard } from './services/auth/manager-guard';
import { CGuard } from './services/auth/c-guard';
import { RegistrationRequestsComponent } from './components/manager/registration-requests/registration-requests';
import { RequestDetailComponent } from './components/manager/request-detail/request-detail';
import { VehicleListComponent } from './components/manager/vehicle-list/vehicle-list';
import { VehicleFormComponent } from './components/manager/vehicle-form/vehicle-form';
import { VehicleDetailComponent } from './components/manager/vehicle-detail/vehicle-detail';
import { WarehouseListComponent } from './components/manager/warehouse-list/warehouse-list';
import { WarehouseFormComponent } from './components/manager/warehouse-form/warehouse-form';
import { WarehouseDetailComponent } from './components/manager/warehouse-detail/warehouse-detail';
import { CreateOrderComponent } from './components/customer/create-order/create-order';
import { MyOrdersComponent } from './components/customer/my-orders/my-orders';
import { ProductCatalogComponent } from './components/customer/product-catalog/product-catalog';
import { ManagerOrdersComponent } from './components/manager/manager-orders/manager-orders';
import { OrderDetailComponent } from './components/manager/order-detail/order-detail';

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
            },
            {
                path: 'manager/requests',
                component: RegistrationRequestsComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/requests/:id',
                component: RequestDetailComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/vehicles',
                component: VehicleListComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/vehicles/new',
                component: VehicleFormComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/vehicles/:id',
                component: VehicleDetailComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/vehicles/:id/edit',
                component: VehicleFormComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/warehouses',
                component: WarehouseListComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/warehouses/new',
                component: WarehouseFormComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/warehouses/:id',
                component: WarehouseDetailComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/warehouses/:id/edit',
                component: WarehouseFormComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/orders',
                component: ManagerOrdersComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'manager/orders/:id',
                component: OrderDetailComponent,
                canActivate: [ManagerGuard]
            },
            {
                path: 'customer/products',
                component: ProductCatalogComponent,
                canActivate: [CGuard]
            },
            {
                path: 'customer/create-order',
                component: CreateOrderComponent,
                canActivate: [CGuard]
            },
            {
                path: 'customer/orders',
                component: MyOrdersComponent,
                canActivate: [CGuard]
            }
        ]
    },
];
