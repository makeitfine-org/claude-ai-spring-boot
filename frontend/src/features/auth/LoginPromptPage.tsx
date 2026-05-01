import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { ThemeToggle } from '@/components/ThemeToggle'

export function LoginPromptPage() {
  const handleSignIn = () => {
    window.location.href = '/oauth2/authorization/keycloak'
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4 relative">
      <div className="absolute top-4 right-4">
        <ThemeToggle />
      </div>
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle>Welcome</CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Button className="w-full" onClick={handleSignIn}>
            Sign in
          </Button>
          <p className="text-sm text-center text-muted-foreground">
            Don&apos;t have an account?{' '}
            <Link to="/register" className="underline underline-offset-4 hover:text-foreground">
              Register
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  )
}
